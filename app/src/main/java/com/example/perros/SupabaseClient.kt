package com.example.perros

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient

object Supabase {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
    }
}