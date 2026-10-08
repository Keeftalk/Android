import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0"

interface CheckInRequest {
  habitId: string
  date?: string // "YYYY-MM-DD" optional, server computes authoritative effective date
  completed: boolean
}

serve(async (req) => {
  try {
    const authHeader = req.headers.get("Authorization")
    if (!authHeader) {
      return new Response(JSON.stringify({ success: false, error: "UNAUTHENTICATED", message: "Missing Authorization header" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      })
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? ""
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? supabaseAnonKey

    const supabaseAuth = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })

    const { data: { user }, error: userError } = await supabaseAuth.auth.getUser()
    if (userError || !user) {
      return new Response(JSON.stringify({ success: false, error: "UNAUTHENTICATED", message: "Invalid or expired session" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      })
    }

    const body: CheckInRequest = await req.json()
    const { habitId, completed } = body

    if (!habitId) {
      return new Response(JSON.stringify({ success: false, error: "INVALID_REQUEST", message: "Missing habitId" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      })
    }

    // Use service role client for authoritative database operations
    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // 1. Fetch habit and verify ownership & active status
    const { data: habit, error: habitError } = await supabaseAdmin
      .from("habits")
      .select("*")
      .eq("id", habitId)
      .single()

    if (habitError || !habit) {
      return new Response(JSON.stringify({ success: false, error: "HABIT_NOT_FOUND", message: "Habit not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" },
      })
    }

    if (habit.user_id !== user.id) {
      return new Response(JSON.stringify({ success: false, error: "HABIT_NOT_OWNED", message: "Unauthorized habit access" }), {
        status: 403,
        headers: { "Content-Type": "application/json" },
      })
    }

    if (habit.deleted) {
      return new Response(JSON.stringify({ success: false, error: "HABIT_INACTIVE", message: "Habit is inactive or deleted" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      })
    }

    // 2. Determine authoritative server time and date
    const now = new Date() // Server time (UTC or configured local timezone)
    const currentHour = now.getUTCHours()
    const currentMin = now.getUTCMinutes()
    const currentTotalMins = currentHour * 60 + currentMin

    const serverDate = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()))
    let effectiveDate = new Date(serverDate)

    const slot = habit.slot || "Morning"

    // 3. Time slot validation & cross-midnight automatic attribution
    if (completed) {
      if (slot === "Late Night") {
        // Late Night slot: 22:00 to 01:00 inclusive (1320 mins to 60 mins next day)
        // If current time is between 00:00 and 01:00 (0 to 60 mins), attribute to previous day
        const isPastMidnightContinuation = currentTotalMins <= 60 // 00:00 to 01:00
        const isEveningLateWindow = currentTotalMins >= 1320 // 22:00 to 23:59

        if (isPastMidnightContinuation) {
          effectiveDate.setUTCDate(effectiveDate.getUTCDate() - 1)
        } else if (!isEveningLateWindow) {
          // Outside 22:00 - 01:00 window
          return new Response(JSON.stringify({ success: false, error: "INVALID_CHECKIN_TIME", message: "Late night habit can only be completed between 22:00 and 01:00" }), {
            status: 400,
            headers: { "Content-Type": "application/json" },
          })
        }
      } else {
        // For other slots or general check-ins, validate slot window ± 1 hour
        const slotRanges: Record<string, [number, number]> = {
          "Early Morning": [180, 540], // 03:00 - 09:00
          "Morning": [420, 750],       // 07:00 - 12:30
          "Midday": [630, 900],        // 10:30 - 15:00
          "Afternoon": [780, 1110],    // 13:00 - 18:30
          "Evening": [990, 1380],      // 16:30 - 23:00
        }

        const range = slotRanges[slot]
        if (range && (currentTotalMins < range[0] || currentTotalMins > range[1])) {
          return new Response(JSON.stringify({ success: false, error: "INVALID_CHECKIN_TIME", message: `Habit not available in ${slot} slot at this time` }), {
            status: 400,
            headers: { "Content-Type": "application/json" },
          })
        }
      }
    }

    const epochDay = Math.floor(effectiveDate.getTime() / (1000 * 60 * 60 * 24))
    const effectiveDateStr = effectiveDate.toISOString().split("T")[0]

    // 4. Perform idempotent upsert into habit_checkins
    const checkinId = `${habitId}_${epochDay}`
    if (completed) {
      const { error: upsertError } = await supabaseAdmin
        .from("habit_checkins")
        .upsert({
          id: checkinId,
          habit_id: habitId,
          epoch_day: epochDay,
          completed: true,
          updated_at: Date.now(),
          deleted: false,
          sync_state: "SYNCED"
        }, { onConflict: "id" })

      if (upsertError) {
        throw upsertError
      }
    } else {
      const { error: deleteError } = await supabaseAdmin
        .from("habit_checkins")
        .delete()
        .eq("id", checkinId)

      if (deleteError) {
        throw deleteError
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        habitId: habitId,
        effectiveDate: effectiveDateStr,
        completed: completed,
      }),
      {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }
    )
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, error: "SERVER_ERROR", message: err.message }),
      {
        status: 500,
        headers: { "Content-Type": "application/json" },
      }
    )
  }
})
