import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2.38.4'

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

    const { purchaseToken, productId } = await req.json()

    // 1. Get current user
    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser(token)

    if (userError || !user) throw new Error('Not authenticated')

    // 2. Plan Mapping
    let plan = 'FREE'
    let limit = 5368709120 // 5GB

    if (productId === 'keeftalk_plus_monthly' || productId === 'keeftalk_plus_yearly') {
        plan = productId === 'keeftalk_plus_monthly' ? 'PLUS_MONTHLY' : 'PLUS_YEARLY'
        limit = 107374182400 // 100GB
    } else if (productId === 'keeftalk_pro_monthly') {
        plan = 'PRO_MONTHLY'
        limit = 536870912000 // 500GB
    } else if (productId === 'keeftalk_family_monthly') {
        plan = 'FAMILY_MONTHLY'
        limit = 2199023255552 // 2TB
    }

    const expiryDate = new Date()
    if (plan.includes('YEARLY')) {
        expiryDate.setFullYear(expiryDate.getFullYear() + 1)
    } else {
        expiryDate.setMonth(expiryDate.getMonth() + 1)
    }

    // 3. Update Entitlement via RPC (Single Source of Truth)
    const { error: rpcError } = await supabaseClient.rpc('sync_subscription_entitlement', {
      target_user_id: user.id,
      new_plan: plan,
      new_limit: limit,
      new_status: 'active',
      new_expiry: expiryDate.toISOString(),
      new_purchase_token: purchaseToken,
      new_order_id: 'GPA.' + Math.random().toString().slice(2, 14) // Placeholder for actual order id from Google
    })

    if (rpcError) throw rpcError

    return new Response(JSON.stringify({ success: true, plan, limit }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 400,
    })
  }
})
