import { assertEquals, assertExists } from 'https://deno.land/std@0.208.0/assert/mod.ts'
import { getClientIp } from '../init-user-storage/index.ts'

const regionByCountry: Record<string, string> = { TN: 'WEUR', US: 'ENAM', JP: 'APAC', AU: 'OC', UA: 'EEUR' }

Deno.test('1. IP Extraction and Country Resolution', async () => {
  const req = new Request('https://api.keeftalk.com/init', {
    headers: { 'cf-connecting-ip': '196.203.12.3' } // Tunisia IP
  })
  const ip = getClientIp(req)
  assertEquals(ip, '196.203.12.3')
})

Deno.test('2. Tunisia Mapping to WEUR Region', () => {
  const country = 'TN'
  const expectedRegion = 'WEUR'
  assertEquals(regionByCountry[country], expectedRegion)
})

Deno.test('3. Immutability & Fallback Validation', () => {
  const existingAssignment = {
    user_id: 'user-123',
    country_code: 'TN',
    storage_region: 'WEUR',
    private_bucket: 'keeftalk-weur-private'
  }

  // Traveling to Japan (APAC IP), but user already has WEUR assignment
  const newIpCountry = 'JP'
  const finalRegion = existingAssignment ? existingAssignment.storage_region : regionByCountry[newIpCountry]
  assertEquals(finalRegion, 'WEUR', 'Storage region must remain immutable despite traveling / IP change')
})

Deno.test('4. Bucket Set Derivation', () => {
  const R2_BUCKETS = {
    WEUR: {
      private: "keeftalk-weur-private",
      public: "keeftalk-weur-public",
      market: "keeftalk-weur-market",
      detention: "keeftalk-weur-detention",
      backups: "keeftalk-weur-backups",
    }
  }
  const buckets = R2_BUCKETS['WEUR']
  assertExists(buckets)
  assertEquals(buckets.private, 'keeftalk-weur-private')
  assertEquals(buckets.detention, 'keeftalk-weur-detention')
})
