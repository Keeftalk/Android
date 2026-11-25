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
    const supabaseKey = Deno.env.get('SUPABASE_ANON_KEY') || ''
    const supabase = createClient(supabaseUrl, supabaseKey)

    // Fetch active backend pricing configuration
    const { data: pricing, error } = await supabase
      .from('billing_pricing')
      .select('*')
      .eq('is_active', true)
      .maybeSingle()

    if (error || !pricing) {
      // Fallback default pricing if table not yet populated
      const defaultPricing = {
        version_name: 'v1.0-default',
        currency: 'USD',
        storage_rate_per_gb_month: 0.015000,
        bandwidth_rate_per_gb: 0.020000,
        read_op_rate_per_10k: 0.003600,
        write_op_rate_per_10k: 0.045000,
        backup_rate_per_gb_month: 0.020000,
        maintenance_allocation_pct: 0.1200,
        founder_margin_pct: 0.0500,
        effective_from: new Date().toISOString()
      }
      return new Response(JSON.stringify(defaultPricing), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      })
    }

    return new Response(JSON.stringify(pricing), {
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
