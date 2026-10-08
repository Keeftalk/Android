import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0"

interface HabitContext {
  action: "recommend_type" | "generate_insight"
  habitName?: string
  category?: string
  amount?: number
  unit?: string
  durationMinutes?: number
  streak?: number
  completedHabits?: Array<{
    name: string
    category: string
    type: string
    amount?: number
    unit?: string
    streak: number
  }>
  recentFingerprints?: string[]
  language?: string
}

interface HabitClassification {
  typeKey: "STARTER" | "CORE" | "ACTIVE" | "TRANSFORMATIVE"
  categoryKey: string
  title: string
  badge: string
  reason: string
  confidence: number
}

function classifyHabitGeneric(
  name: string,
  category?: string,
  amount?: number,
  unit?: string,
  durationMinutes?: number
): HabitClassification {
  const lowerName = (name || "").toLowerCase()
  const un = (unit || "").toLowerCase()
  const amt = amount || 0
  const dur = durationMinutes || (un.includes("min") ? amt : 15)

  let cat = (category || "OTHER").toUpperCase()
  if (cat === "OTHER" || !cat) {
    if (lowerName.includes("run") || lowerName.includes("gym") || lowerName.includes("workout") || lowerName.includes("swim") || lowerName.includes("cycle") || lowerName.includes("stretch") || lowerName.includes("yoga") || lowerName.includes("exercise")) {
      cat = "FITNESS"
    } else if (lowerName.includes("read") || lowerName.includes("study") || lowerName.includes("learn") || lowerName.includes("vocabulary") || lowerName.includes("spanish") || lowerName.includes("code") || lowerName.includes("programming")) {
      cat = "LEARNING"
    } else if (lowerName.includes("meditat") || lowerName.includes("breathe") || lowerName.includes("pray") || lowerName.includes("mindful")) {
      cat = "MINDFULNESS"
    } else if (lowerName.includes("water") || lowerName.includes("vitamin") || lowerName.includes("sleep")) {
      cat = "HEALTH"
    } else if (lowerName.includes("floss") || lowerName.includes("brush") || lowerName.includes("shower") || lowerName.includes("skincare")) {
      cat = "PERSONAL_CARE"
    } else if (lowerName.includes("clean") || lowerName.includes("tidy") || lowerName.includes("bed")) {
      cat = "HOUSEHOLD"
    } else if (lowerName.includes("save") || lowerName.includes("budget") || lowerName.includes("money")) {
      cat = "FINANCE"
    } else if (lowerName.includes("write") || lowerName.includes("draw") || lowerName.includes("guitar") || lowerName.includes("piano")) {
      cat = "CREATIVE"
    } else if (lowerName.includes("call") || lowerName.includes("family") || lowerName.includes("friend")) {
      cat = "SOCIAL"
    } else if (lowerName.includes("meal") || lowerName.includes("cook") || lowerName.includes("fruit")) {
      cat = "NUTRITION"
    } else {
      cat = "PRODUCTIVITY"
    }
  }

  let score = 0
  if (dur > 60 || lowerName.includes("marathon") || lowerName.includes("ironman") || lowerName.includes("master")) {
    score += 35
  } else if (dur >= 30 || lowerName.includes("gym") || lowerName.includes("workout") || lowerName.includes("run")) {
    score += 22
  } else if (dur >= 15 || lowerName.includes("read") || lowerName.includes("study") || lowerName.includes("code")) {
    score += 15
  } else {
    score += 5
  }

  if (["FITNESS", "LEARNING", "FINANCE", "CREATIVE"].includes(cat)) {
    score += 10
  }

  let typeKey: "STARTER" | "CORE" | "ACTIVE" | "TRANSFORMATIVE" = "STARTER"
  let title = "Starter Habit"
  let badge = "QUICK WIN"
  let reason = "Small, accessible daily actions that are easy to start and repeat with very low friction."

  if (score >= 30 || lowerName.includes("marathon") || lowerName.includes("transform")) {
    typeKey = "TRANSFORMATIVE"
    title = "Transformative Habit"
    badge = "MAJOR TRANSFORMATION"
    reason = "Big lifestyle and long-term capability changes built through sustained patience and multi-month commitment."
  } else if (score >= 20) {
    typeKey = "ACTIVE"
    title = "Active Lifestyle"
    badge = "LONG-TERM GROWTH"
    reason = "Habits requiring physical or cognitive exertion, structured planning, and consistent training sessions."
  } else if (score >= 10) {
    typeKey = "CORE"
    title = "Core Routine"
    badge = "BUILD MOMENTUM"
    reason = "Everyday knowledge and mental routines that compound through consistent daily repetition."
  }

  return {
    typeKey,
    categoryKey: cat,
    title,
    badge,
    reason,
    confidence: 0.92,
  }
}

function calculateMetric(amount: number, unit: string): string {
  const un = unit.toLowerCase().trim()
  if (["page", "pages", "p"].includes(un)) {
    const annual = amount * 365
    const books = Math.max(1, Math.round(annual / 350))
    return `📚 ${amount} ${un} today. At this daily rate, you'll reach about ${annual.toLocaleString()} pages over a year — roughly ${books} average-length books!`
  }
  if (["min", "mins", "minute", "minutes", "m"].includes(un)) {
    const weekly = amount * 7
    const monthly = amount * 30
    return `⏱️ ${amount} ${un} today. Stacking this consistently gives you ${weekly} minutes weekly (~${Math.round(monthly / 60)} hours monthly)!`
  }
  if (["km", "kilometer", "kilometers", "mi", "mile", "miles"].includes(un)) {
    const weekly = amount * 7
    return `🏃 ${amount} ${un} today. Your weekly distance is pacing at ${weekly} ${un}/week.`
  }
  if (["word", "words", "w"].includes(un)) {
    const annual = amount * 365
    return `✍️ ${amount} ${un} today. That accumulates to about ${annual.toLocaleString()} words over a year!`
  }
  if (["step", "steps"].includes(un)) {
    const weekly = amount * 7
    return `👟 ${amount.toLocaleString()} ${un} today. Keep moving — that's ~${weekly.toLocaleString()} steps weekly!`
  }
  if (["rep", "reps", "set", "sets"].includes(un)) {
    const weekly = amount * 7
    return `💪 ${amount} ${un} today. Consistent repetition builds real strength.`
  }
  if (["dollar", "dollars", "usd", "$", "eur", "€", "gbp", "£"].includes(un)) {
    const annual = amount * 365
    return `💰 Saved/invested ${amount} ${un} today. That builds to about ${annual.toLocaleString()} ${un} over a year!`
  }
  const annual = amount * 365
  return `📈 ${amount} ${un} completed today. At this daily rate, you'll accumulate about ${annual.toLocaleString()} ${un} over a full year!`
}

serve(async (req) => {
  try {
    const authHeader = req.headers.get("Authorization")
    if (!authHeader) {
      return new Response(JSON.stringify({ success: false, error: "UNAUTHENTICATED", message: "Missing auth header" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      })
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? ""
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? ""
    const supabaseAuth = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    })

    const { data: { user }, error: userError } = await supabaseAuth.auth.getUser()
    if (userError || !user) {
      return new Response(JSON.stringify({ success: false, error: "UNAUTHENTICATED", message: "Invalid session" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      })
    }

    const body: HabitContext = await req.json()
    const { action, habitName, category, amount, unit, durationMinutes, streak, completedHabits, recentFingerprints = [], language = "en" } = body

    if (action === "recommend_type") {
      if (!habitName || typeof habitName !== "string" || habitName.trim().length === 0) {
        return new Response(JSON.stringify({ success: false, error: "INVALID_REQUEST", message: "Invalid habitName" }), {
          status: 400,
          headers: { "Content-Type": "application/json" },
        })
      }
      const rec = classifyHabitGeneric(habitName, category, amount, unit, durationMinutes)
      return new Response(JSON.stringify({ success: true, recommendation: rec }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      })
    }

    const habitsList = completedHabits || (habitName ? [{ name: habitName, category: category || "OTHER", type: "STARTER", amount, unit, streak: streak || 1 }] : [])

    if (habitsList.length === 0) {
      return new Response(
        JSON.stringify({ success: true, insight: "Build your life, one habit at a time.", fingerprint: "default_empty" }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      )
    }

    const candidateInsights: Array<{ text: string; fingerprint: string }> = []

    if (habitsList.length > 1) {
      const namesStr = habitsList.map(h => h.name).join(", ")
      const fp = `multi_${habitsList.length}_${habitsList.map(h => h.name).join("_")}`
      candidateInsights.push({
        text: language === "fr"
          ? `🎉 ${namesStr} — c'est une excellente combinaison d'actions quotidiennes complétées aujourd'hui !`
          : `🎉 ${namesStr} — that's a powerful combination of daily routines completed today!`,
        fingerprint: fp
      })
    }

    for (const h of habitsList) {
      if (h.amount && h.unit) {
        const metricText = calculateMetric(h.amount, h.unit)
        candidateInsights.push({
          text: metricText,
          fingerprint: `metric_${h.name}_${h.amount}_${h.unit}`
        })
      }
      if (h.streak >= 7) {
        candidateInsights.push({
          text: `🔥 You've maintained a ${h.streak}-day streak on "${h.name}". Consistency is compounding into automatic routine!`,
          fingerprint: `streak_${h.name}_${h.streak}`
        })
      }
      candidateInsights.push({
        text: `⭐ Completed "${h.name}" today. Every small repetition strengthens your routine.`,
        fingerprint: `general_${h.name}`
      })
    }

    let selected = candidateInsights.find(ci => !recentFingerprints.includes(ci.fingerprint))
    if (!selected && candidateInsights.length > 0) {
      selected = candidateInsights[0]
    }

    const finalInsight = selected ? selected.text : `⭐ All daily habits completed successfully!`
    const finalFingerprint = selected ? selected.fingerprint : `default_all_done`

    return new Response(
      JSON.stringify({
        success: true,
        insight: finalInsight,
        fingerprint: finalFingerprint,
        generatedAt: Date.now(),
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    )
  } catch (err) {
    return new Response(
      JSON.stringify({ success: false, error: "SERVER_ERROR", message: "An internal error occurred" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    )
  }
})
