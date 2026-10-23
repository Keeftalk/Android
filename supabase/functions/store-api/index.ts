import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { AwsClient } from 'https://esm.sh/aws4fetch@1.0.17'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
}

const R2_ACCESS_KEY_ID = Deno.env.get('R2_ACCESS_KEY_ID') || 'mock_access_key'
const R2_SECRET_ACCESS_KEY = Deno.env.get('R2_SECRET_ACCESS_KEY') || 'mock_secret_key'
const R2_ENDPOINT = Deno.env.get('R2_ENDPOINT') || 'https://5cfb75b2169676dc91bd6a57b7d6d7ab.r2.cloudflarestorage.com'
const PUBLIC_R2_BUCKET = Deno.env.get('STORE_PUBLIC_BUCKET') || 'keeftalk-store-public'
const PRIVATE_R2_BUCKET = Deno.env.get('STORE_PRIVATE_BUCKET') || 'keeftalk-store-private'

const r2 = new AwsClient({
  accessKeyId: R2_ACCESS_KEY_ID,
  secretAccessKey: R2_SECRET_ACCESS_KEY,
  service: 's3',
  region: 'auto',
})

export const getR2Url = (bkt: string, pth: string) => {
  const urlObj = new URL(R2_ENDPOINT)
  const url = new URL(urlObj.origin)
  if (!url.pathname.endsWith('/')) url.pathname += '/'
  url.pathname += `${bkt}/${pth.startsWith('/') ? pth.slice(1) : pth}`
  return url
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
    if (!supabaseUrl || !supabaseKey) {
      return new Response(JSON.stringify({ error: 'Server configuration error' }), { status: 500, headers: corsHeaders })
    }

    const supabase = createClient(supabaseUrl, supabaseKey)
    const url = new URL(req.url)
    const pathname = url.pathname.replace(/\/+$/, '')

    // Extract endpoint path
    const action = pathname.split('/').pop() || 'catalog'

    // Authenticate user if authorization header provided
    const authHeader = req.headers.get('Authorization')
    let user = null
    if (authHeader) {
      const token = authHeader.split(' ')[1]
      if (token) {
        const { data } = await supabase.auth.getUser(token)
        user = data?.user || null
      }
    }

    const body = req.method === 'POST' ? await req.json().catch(() => ({})) : {}

    // 1. ACTION: catalog (GET or POST)
    if (action === 'catalog' || action === 'store-catalog') {
      const category = url.searchParams.get('category') || body.category || 'all'
      const search = url.searchParams.get('q') || body.query || ''
      const limit = parseInt(url.searchParams.get('limit') || body.limit || '30', 10)
      const country = url.searchParams.get('country') || body.country || 'GLOBAL'

      let query = supabase
        .from('store_extensions')
        .select(`
          *,
          publisher:store_publishers(id, display_name, is_verified)
        `)
        .eq('status', 'PUBLISHED')

      if (category && category !== 'all') {
        query = query.eq('category', category.toLowerCase())
      }

      if (search) {
        query = query.or(`name.ilike.%${search}%,short_description.ilike.%${search}%,id.ilike.%${search}%`)
      }

      query = query.order('download_count', { ascending: false }).limit(limit)

      const { data: extensions, error } = await query
      if (error) throw error

      return new Response(JSON.stringify({
        success: true,
        category,
        country,
        extensions: extensions || []
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 2. ACTION: rankings (GET or POST)
    if (action === 'rankings' || action === 'store-rankings') {
      const mode = url.searchParams.get('mode') || body.mode || 'global' // 'global' or 'country'
      const countryCode = url.searchParams.get('country') || body.country || 'TN'

      let query = supabase
        .from('store_extensions')
        .select(`
          *,
          publisher:store_publishers(id, display_name, is_verified)
        `)
        .eq('status', 'PUBLISHED')

      if (mode === 'global') {
        query = query.order('download_count', { ascending: false }).limit(20)
      } else {
        // Country-specific ranking
        query = query.order('install_count', { ascending: false }).limit(20)
      }

      const { data, error } = await query
      if (error) throw error

      return new Response(JSON.stringify({
        success: true,
        mode,
        countryCode,
        rankings: data || []
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 3. ACTION: details (GET or POST)
    if (action === 'details' || action === 'store-extension-details') {
      const extensionId = url.searchParams.get('id') || body.extensionId
      if (!extensionId) {
        return new Response(JSON.stringify({ error: 'Missing extensionId parameter' }), { status: 400, headers: corsHeaders })
      }

      const { data: extension, error: extError } = await supabase
        .from('store_extensions')
        .select(`
          *,
          publisher:store_publishers(id, display_name, description, website, is_verified),
          releases:store_releases(*),
          media:store_media_assets(*)
        `)
        .eq('id', extensionId)
        .maybeSingle()

      if (extError || !extension) {
        return new Response(JSON.stringify({ error: 'Extension not found' }), { status: 404, headers: corsHeaders })
      }

      return new Response(JSON.stringify({
        success: true,
        extension
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 4. ACTION: download (GET or POST)
    if (action === 'download' || action === 'store-download') {
      const extensionId = url.searchParams.get('id') || body.extensionId
      const version = url.searchParams.get('version') || body.version

      if (!extensionId) {
        return new Response(JSON.stringify({ error: 'Missing extensionId' }), { status: 400, headers: corsHeaders })
      }

      // Fetch release
      let relQuery = supabase
        .from('store_releases')
        .select('*')
        .eq('extension_id', extensionId)
        .eq('status', 'PUBLISHED')

      if (version) {
        relQuery = relQuery.eq('version', version)
      } else {
        relQuery = relQuery.order('created_at', { ascending: false }).limit(1)
      }

      const { data: releases, error } = await relQuery
      const release = releases?.[0]

      if (error || !release) {
        return new Response(JSON.stringify({ error: 'Release package not found for extension' }), { status: 404, headers: corsHeaders })
      }

      // Increment download counter
      await supabase.rpc('increment_extension_download', { p_extension_id: extensionId }).catch(() => {
        supabase.from('store_extensions').update({
          download_count: (release.download_count || 0) + 1
        }).eq('id', extensionId)
      })

      // Generate presigned GET URL for public package
      const packageUrl = getR2Url(PUBLIC_R2_BUCKET, release.package_object_key)
      const signedRequest = await r2.sign(packageUrl.toString(), {
        method: 'GET',
        aws: { signQuery: true }
      })

      return new Response(JSON.stringify({
        success: true,
        downloadUrl: signedRequest.url,
        version: release.version,
        sha256: release.package_hash_sha256,
        sizeBytes: release.package_size_bytes
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 5. ACTION: request-install (POST)
    if (action === 'request-install' || action === 'store-request-install') {
      if (!user) {
        return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders })
      }

      const { extensionId, version, deviceId } = body
      if (!extensionId) {
        return new Response(JSON.stringify({ error: 'Missing extensionId' }), { status: 400, headers: corsHeaders })
      }

      const { data: request, error } = await supabase
        .from('store_device_install_requests')
        .insert({
          user_id: user.id,
          extension_id: extensionId,
          version: version || '1.0.0',
          device_id: deviceId || 'linked_android_device',
          status: 'PENDING'
        })
        .select()
        .single()

      if (error) throw error

      return new Response(JSON.stringify({
        success: true,
        requestId: request.id,
        status: request.status,
        expiresAt: request.expires_at
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 6. ACTION: install-status (GET or POST)
    if (action === 'install-status' || action === 'store-install-status') {
      if (!user) {
        return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders })
      }

      const requestId = url.searchParams.get('requestId') || body.requestId
      if (requestId) {
        const { data, error } = await supabase
          .from('store_device_install_requests')
          .select('*')
          .eq('id', requestId)
          .eq('user_id', user.id)
          .single()

        if (error) throw error
        return new Response(JSON.stringify({ success: true, request: data }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
      }

      // Fetch pending requests for user
      const { data, error } = await supabase
        .from('store_device_install_requests')
        .select('*')
        .eq('user_id', user.id)
        .eq('status', 'PENDING')
        .gt('expires_at', new Date().toISOString())

      if (error) throw error
      return new Response(JSON.stringify({ success: true, pendingRequests: data || [] }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 7. ACTION: rate-extension (POST)
    if (action === 'rate-extension' || action === 'store-rate-extension') {
      if (!user) {
        return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders })
      }

      const { extensionId, rating, reviewText } = body
      if (!extensionId || !rating || rating < 1 || rating > 5) {
        return new Response(JSON.stringify({ error: 'Invalid extensionId or rating (1..5)' }), { status: 400, headers: corsHeaders })
      }

      const { data, error } = await supabase
        .from('store_ratings')
        .upsert({
          extension_id: extensionId,
          user_id: user.id,
          rating: rating,
          review_text: reviewText || '',
          updated_at: new Date().toISOString()
        }, { onConflict: 'extension_id,user_id' })
        .select()
        .single()

      if (error) throw error

      return new Response(JSON.stringify({ success: true, rating: data }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    // 8. ACTION: publisher-analytics (GET or POST)
    if (action === 'publisher-analytics') {
      if (!user) {
        return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers: corsHeaders })
      }

      // Verify user is a publisher
      const { data: publisher } = await supabase
        .from('store_publishers')
        .select('*')
        .eq('user_id', user.id)
        .maybeSingle()

      if (!publisher) {
        return new Response(JSON.stringify({ error: 'Forbidden: User is not an approved publisher' }), { status: 403, headers: corsHeaders })
      }

      const { data: extensions } = await supabase
        .from('store_extensions')
        .select('id, name, download_count, install_count, rating_avg, rating_count, status')
        .eq('publisher_id', publisher.id)

      const totalDownloads = (extensions || []).reduce((acc, curr) => acc + (parseInt(curr.download_count || 0)), 0)
      const totalInstalls = (extensions || []).reduce((acc, curr) => acc + (parseInt(curr.install_count || 0)), 0)

      return new Response(JSON.stringify({
        success: true,
        publisher,
        totalDownloads,
        totalInstalls,
        extensions: extensions || []
      }), { headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
    }

    return new Response(JSON.stringify({ error: `Unknown endpoint action: ${action}` }), { status: 404, headers: corsHeaders })

  } catch (err: any) {
    console.error('Store API Function Error:', err)
    return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } })
  }
})
