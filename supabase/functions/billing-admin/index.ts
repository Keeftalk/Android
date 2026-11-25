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
      return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders })
    }

    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid token' }), { status: 401, headers: corsHeaders })
    }

    // Verify admin privileges via app metadata
    const isAdmin = user.app_metadata?.is_admin === true || user.app_metadata?.role === 'admin'
    if (!isAdmin) {
      return new Response(JSON.stringify({ error: 'Forbidden: Admin privileges required' }), { status: 403, headers: corsHeaders })
    }

    const body = await req.json().catch(() => ({}))
    const { action, versionName, rates } = body

    if (action === 'create-pricing-version') {
      if (!versionName || !rates) {
        return new Response(JSON.stringify({ error: 'Missing versionName or rates' }), { status: 400, headers: corsHeaders })
      }

      // Deactivate current pricing versions
      await supabase.from('billing_pricing').update({ is_active: false, effective_until: new Date().toISOString() }).eq('is_active', true)

      // Insert new active pricing version
      const { data: newPricing, error: insertError } = await supabase.from('billing_pricing').insert({
        version_name: versionName,
        currency: rates.currency || 'USD',
        storage_rate_per_gb_month: rates.storage_rate_per_gb_month,
        bandwidth_rate_per_gb: rates.bandwidth_rate_per_gb,
        read_op_rate_per_10k: rates.read_op_rate_per_10k,
        write_op_rate_per_10k: rates.write_op_rate_per_10k,
        backup_rate_per_gb_month: rates.backup_rate_per_gb_month,
        maintenance_allocation_pct: rates.maintenance_allocation_pct,
        founder_margin_pct: rates.founder_margin_pct,
        is_active: true
      }).select().single()

      if (insertError) throw insertError

      // Log in audit log
      await supabase.from('billing_audit_log').insert({
        admin_id: user.id,
        action: 'CREATE_PRICING_VERSION',
        pricing_version_id: newPricing.id,
        changes: rates
      })

      return new Response(JSON.stringify({ success: true, pricing: newPricing }), { status: 200, headers: corsHeaders })
    }

    return new Response(JSON.stringify({ error: 'Invalid action' }), { status: 400, headers: corsHeaders })

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), { status: 400, headers: corsHeaders })
  }
})
