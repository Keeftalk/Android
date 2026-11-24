import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const { purchaseToken, productId, topupAmount } = await req.json()

    // 1. Get current user
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser(token)

    if (userError || !user) throw new Error('Not authenticated')

    // Determine top-up amount in USD (default to $5.00 if topupAmount not specified)
    const amountToAdd = Number(topupAmount) || 5.00
    const idempotencyKey = `topup_${purchaseToken || Math.random().toString(36).substring(7)}`

    // Record top-up in billing_payments
    await supabaseClient.from('billing_payments').insert({
      user_id: user.id,
      amount: amountToAdd,
      currency: 'USD',
      provider: 'IN_APP_PURCHASE',
      provider_transaction_id: purchaseToken || idempotencyKey,
      status: 'SUCCESS',
      idempotency_key: idempotencyKey
    }).catch(() => null)

    // Increment prepaid balance in billing_balances
    const { data: balanceRec } = await supabaseClient.from('billing_balances')
      .select('prepaid_balance')
      .eq('user_id', user.id)
      .maybeSingle()

    const currentBal = balanceRec?.prepaid_balance || 0.0
    const newBal = currentBal + amountToAdd

    await supabaseClient.from('billing_balances').upsert({
      user_id: user.id,
      prepaid_balance: newBal,
      currency: 'USD',
      updated_at: new Date().toISOString()
    })

    // Log transaction in billing_ledger
    await supabaseClient.from('billing_ledger').insert({
      user_id: user.id,
      transaction_type: 'PREPAID_TOPUP',
      base_amount: amountToAdd,
      total_amount: amountToAdd,
      currency: 'USD',
      description: `Prepaid Cloud Balance Top-up ($${amountToAdd.toFixed(2)})`,
      idempotency_key: idempotencyKey
    }).catch(() => null)

    return new Response(JSON.stringify({ success: true, new_balance: newBal, topup_added: amountToAdd }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (error: any) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 400,
    })
  }
})
