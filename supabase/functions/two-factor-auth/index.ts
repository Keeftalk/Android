import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.7.1"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface RequestBody {
  action: 'send_sms_otp' | 'send_email_otp' | 'verify_challenge';
  phone_number?: string;
  email?: string;
  code?: string;
  challenge_id?: string;
  purpose?: string;
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? ''
    const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY') ?? ''
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''

    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Missing Authorization header' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey, {
      auth: { persistSession: false }
    })

    const body: RequestBody = await req.json()

    if (body.action === 'send_sms_otp') {
      const twilioSid = Deno.env.get('TWILIO_ACCOUNT_SID')
      const twilioAuthToken = Deno.env.get('TWILIO_AUTH_TOKEN')
      const twilioFrom = Deno.env.get('TWILIO_PHONE_NUMBER')

      if (!twilioSid || !twilioAuthToken || !twilioFrom) {
        return new Response(JSON.stringify({
          error: 'SMS provider unconfigured. TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER environment variables are required in Supabase Dashboard.',
          configured: false
        }), {
          status: 501,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        })
      }

      // Real Twilio API Call
      const credentials = btoa(`${twilioSid}:${twilioAuthToken}`)
      const twilioUrl = `https://api.twilio.com/2010-04-01/Accounts/${twilioSid}/Messages.json`

      const params = new URLSearchParams()
      params.append('To', body.phone_number || '')
      params.append('From', twilioFrom)
      params.append('Body', `Your Keeftalk security code is: ${body.code}. Do not share this code with anyone.`)

      const twilioRes = await fetch(twilioUrl, {
        method: 'POST',
        headers: {
          'Authorization': `Basic ${credentials}`,
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: params.toString()
      })

      const twilioData = await twilioRes.json()

      if (!twilioRes.ok) {
        return new Response(JSON.stringify({
          error: `Twilio delivery failed: ${twilioData.message || 'Unknown error'}`,
          code: twilioData.code
        }), {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        })
      }

      return new Response(JSON.stringify({
        success: true,
        message: 'SMS OTP sent via Twilio',
        sid: twilioData.sid
      }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    return new Response(JSON.stringify({ error: 'Action not supported' }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })

  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})
