import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// 5 Logical Regions & 5 Buckets Each
const R2_BUCKETS: Record<string, { private: string; public: string; market: string; detention: string; backups: string }> = {
  WEUR: {
    private: "keeftalk-weur-private",
    public: "keeftalk-weur-public",
    market: "keeftalk-weur-market",
    detention: "keeftalk-weur-detention",
    backups: "keeftalk-weur-backups",
  },
  EEUR: {
    private: "keeftalk-eeur-private",
    public: "keeftalk-eeur-public",
    market: "keeftalk-eeur-market",
    detention: "keeftalk-eeur-detention",
    backups: "keeftalk-eeur-backups",
  },
  ENAM: {
    private: "keeftalk-enam-private",
    public: "keeftalk-enam-public",
    market: "keeftalk-enam-market",
    detention: "keeftalk-enam-detention",
    backups: "keeftalk-enam-backups",
  },
  APAC: {
    private: "keeftalk-apac-private",
    public: "keeftalk-apac-public",
    market: "keeftalk-apac-market",
    detention: "keeftalk-apac-detention",
    backups: "keeftalk-apac-backups",
  },
  OC: {
    private: "keeftalk-oc-private",
    public: "keeftalk-oc-public",
    market: "keeftalk-oc-market",
    detention: "keeftalk-oc-detention",
    backups: "keeftalk-oc-backups",
  },
};

// Centralized Country -> Region mapping table
const REGION_BY_COUNTRY: Record<string, string> = {
  // Western Europe (WEUR)
  TN: "WEUR", FR: "WEUR", DE: "WEUR", GB: "WEUR", IT: "WEUR", ES: "WEUR", PT: "WEUR", NL: "WEUR", BE: "WEUR", CH: "WEUR", AT: "WEUR", SE: "WEUR", NO: "WEUR", DK: "WEUR", FI: "WEUR", IE: "WEUR", GR: "WEUR", PL: "WEUR",
  // Eastern Europe (EEUR)
  UA: "EEUR", RO: "EEUR", CZ: "EEUR", HU: "EEUR", BG: "EEUR", SK: "EEUR", RS: "EEUR", HR: "EEUR", BY: "EEUR", MD: "EEUR",
  // Eastern North America (ENAM)
  US: "ENAM", CA: "ENAM", MX: "ENAM", BR: "ENAM", AR: "ENAM", CO: "ENAM", CL: "ENAM", PE: "ENAM",
  // Asia-Pacific (APAC)
  JP: "APAC", KR: "APAC", SG: "APAC", IN: "APAC", CN: "APAC", HK: "APAC", TW: "APAC", ID: "APAC", TH: "APAC", VN: "APAC", PH: "APAC", MY: "APAC", PK: "APAC", BD: "APAC", AE: "APAC", SA: "APAC", IL: "APAC", TR: "APAC", ZA: "APAC", NG: "APAC", KE: "APAC", EG: "APAC", MA: "APAC",
  // Oceania (OC)
  AU: "OC", NZ: "OC", FJ: "OC", PG: "OC"
};

const DEFAULT_REGION = "WEUR";
const DEFAULT_COUNTRY = "TN";

export function getClientIp(req: Request): string | null {
  return (
    req.headers.get('cf-connecting-ip') ||
    req.headers.get('x-real-ip') ||
    req.headers.get('x-forwarded-for')?.split(',')[0].trim() ||
    null
  );
}

export async function resolveCountryFromIp(ip: string | null): Promise<string> {
  if (!ip || ip === '127.0.0.1' || ip === 'localhost' || ip.startsWith('192.168.') || ip.startsWith('10.')) {
    return DEFAULT_COUNTRY;
  }
  try {
    // Use Cloudflare cf-ipcountry header if available in request context or fallback
    const res = await fetch(`https://ipapi.co/${ip}/country/`, {
      headers: { 'User-Agent': 'Keeftalk-Storage-Router' }
    });
    if (res.ok) {
      const country = (await res.text()).trim().toUpperCase();
      if (country.length === 2) return country;
    }
  } catch (e) {
    console.warn(`IP Geolocation lookup failed for IP ${ip}:`, e);
  }
  return DEFAULT_COUNTRY;
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
    if (!supabaseUrl || !supabaseKey) {
      throw new Error('Missing Supabase environment variables')
    }

    const supabase = createClient(supabaseUrl, supabaseKey)

    const authHeader = req.headers.get('Authorization')
    const token = authHeader?.split(' ')[1]
    if (!token) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Missing token' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return new Response(JSON.stringify({ error: 'Unauthorized: Invalid user' }), { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 1. Check if user already has an assignment (Immutability guarantee)
    const { data: existingAssignment, error: fetchErr } = await supabase
      .from('user_storage_regions')
      .select('*')
      .eq('user_id', user.id)
      .maybeSingle()

    if (!fetchErr && existingAssignment) {
      console.log(`Returning existing immutable storage assignment for user ${user.id} -> Region: ${existingAssignment.storage_region}`)
      return new Response(JSON.stringify({
        storage_region: existingAssignment.storage_region,
        country_code: existingAssignment.country_code,
        buckets: {
          private: existingAssignment.private_bucket,
          public: existingAssignment.public_bucket,
          market: existingAssignment.market_bucket,
          detention: existingAssignment.detention_bucket,
          backups: existingAssignment.backups_bucket
        },
        assignment_version: existingAssignment.assignment_version
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 2. Obtain request IP (Cloudflare / Supabase deployment headers)
    const clientIp = getClientIp(req) || req.headers.get('cf-ipcountry') ? null : getClientIp(req);
    let countryCode = req.headers.get('cf-ipcountry')?.toUpperCase();

    if (!countryCode || countryCode.length !== 2) {
      countryCode = await resolveCountryFromIp(clientIp);
    }

    // 3. Map country -> region
    let region = REGION_BY_COUNTRY[countryCode]
    if (!region) {
      region = DEFAULT_REGION
    }

    const bucketSet = R2_BUCKETS[region] || R2_BUCKETS[DEFAULT_REGION]

    // 4. Atomically persist assignment via RPC
    const { data: assignedRows, error: rpcErr } = await supabase.rpc('get_or_assign_user_storage_region', {
      p_user_id: user.id,
      p_country_code: countryCode,
      p_storage_region: region,
      p_private_bucket: bucketSet.private,
      p_public_bucket: bucketSet.public,
      p_market_bucket: bucketSet.market,
      p_detention_bucket: bucketSet.detention,
      p_backups_bucket: bucketSet.backups
    })

    if (rpcErr || !assignedRows || assignedRows.length === 0) {
      throw new Error(`Failed to assign storage region: ${rpcErr?.message || 'Unknown error'}`)
    }

    const assignment = assignedRows[0]
    console.log(`Created new storage assignment for user ${user.id} | Country: ${countryCode} | Region: ${region}`)

    return new Response(JSON.stringify({
      storage_region: assignment.storage_region,
      country_code: assignment.country_code,
      buckets: {
        private: assignment.private_bucket,
        public: assignment.public_bucket,
        market: assignment.market_bucket,
        detention: assignment.detention_bucket,
        backups: assignment.backups_bucket
      },
      assignment_version: assignment.assignment_version
    }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })

  } catch (err: any) {
    console.error('Init User Storage Error:', err)
    return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
