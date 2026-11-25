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
    const supabaseUrl = Deno.env.get('SUPABASE_URL') || ''
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || ''
    const supabase = createClient(supabaseUrl, supabaseKey)

    const authHeader = req.headers.get('Authorization')
    const token = authHeader?.replace('Bearer ', '')
    if (!token) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Missing token' }), { status: 401, headers: corsHeaders })
    }

    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid token' }), { status: 401, headers: corsHeaders })
    }

    // 1. Fetch user prepaid balance
    let { data: balanceRecord } = await supabase
      .from('billing_balances')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle()

    if (!balanceRecord) {
      balanceRecord = { user_id: user.id, prepaid_balance: 0.0, currency: 'USD', low_balance_threshold: 2.0 }
    }

    // 2. Fetch user billing preferences
    let { data: preferences } = await supabase
      .from('billing_preferences')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle()

    if (!preferences) {
      preferences = {
        payment_mode: 'PREPAID',
        credit_limit: 50.0,
        spending_limit: 100.0,
        spending_alert_threshold: 80.0,
        auto_topup_enabled: false
      }
    }

    // 3. Calculate active storage consumption from verified R2 objects
    const { data: fileStats } = await supabase
      .from('files')
      .select('file_size, physical_size')
      .eq('owner_id', user.id)
      .neq('status', 'DELETED')

    const totalStorageBytes = (fileStats || []).reduce((acc: number, f: any) => acc + (f.physical_size || f.file_size || 0), 0)

    // 4. Fetch recent usage telemetry entries
    const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString()
    const { data: usageLogs } = await supabase
      .from('billing_usage')
      .select('*')
      .eq('user_id', user.id)
      .gte('period_start', thirtyDaysAgo)

    const totalBandwidthBytes = (usageLogs || [])
      .filter((u: any) => u.resource_type === 'BANDWIDTH')
      .reduce((acc: number, u: any) => acc + (u.quantity || 0), 0)

    const totalReadOps = (usageLogs || [])
      .filter((u: any) => u.resource_type === 'READ_OPS')
      .reduce((acc: number, u: any) => acc + (u.quantity || 0), 0)

    const totalWriteOps = (usageLogs || [])
      .filter((u: any) => u.resource_type === 'WRITE_OPS')
      .reduce((acc: number, u: any) => acc + (u.quantity || 0), 0)

    // 5. Compute real-time cost estimate
    const { data: pricing } = await supabase
      .from('billing_pricing')
      .select('*')
      .eq('is_active', true)
      .maybeSingle()

    const storageGb = totalStorageBytes / (1024 * 1024 * 1024)
    const bandwidthGb = totalBandwidthBytes / (1024 * 1024 * 1024)

    const storageRate = pricing?.storage_rate_per_gb_month || 0.015
    const bandwidthRate = pricing?.bandwidth_rate_per_gb || 0.02
    const readRate = (pricing?.read_op_rate_per_10k || 0.0036) / 10000
    const writeRate = (pricing?.write_op_rate_per_10k || 0.045) / 10000

    const maintPct = pricing?.maintenance_allocation_pct || 0.12
    const marginPct = pricing?.founder_margin_pct || 0.05

    const baseCost = (storageGb * storageRate) + (bandwidthGb * bandwidthRate) + (totalReadOps * readRate) + (totalWriteOps * writeRate)
    const maintenanceFee = baseCost * maintPct
    const founderMargin = baseCost * marginPct
    const estimatedTotal = baseCost + maintenanceFee + founderMargin

    const payload = {
      user_id: user.id,
      prepaid_balance: balanceRecord.prepaid_balance,
      low_balance_threshold: balanceRecord.low_balance_threshold,
      payment_mode: preferences.payment_mode,
      credit_limit: preferences.credit_limit,
      spending_limit: preferences.spending_limit,
      spending_alert_threshold: preferences.spending_alert_threshold,
      current_usage: {
        storage_bytes: totalStorageBytes,
        storage_gb: Number(storageGb.toFixed(4)),
        bandwidth_bytes: totalBandwidthBytes,
        bandwidth_gb: Number(bandwidthGb.toFixed(4)),
        read_operations: totalReadOps,
        write_operations: totalWriteOps
      },
      estimated_charges: {
        base_cost: Number(baseCost.toFixed(6)),
        maintenance_fee: Number(maintenanceFee.toFixed(6)),
        founder_margin: Number(founderMargin.toFixed(6)),
        total_estimated: Number(estimatedTotal.toFixed(6)),
        currency: pricing?.currency || 'USD'
      }
    }

    return new Response(JSON.stringify(payload), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 400,
    })
  }
})
