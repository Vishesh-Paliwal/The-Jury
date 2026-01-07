package com.example.the_jury.di

import com.example.the_jury.Config
import com.example.the_jury.repo.PersonaRepository
import com.example.the_jury.service.AgentRunnerService
import org.koin.dsl.module

val appModule = module {
    single { PersonaRepository() }
    
    // Get API key from platform-specific Config
    single { 
        val apiKey = Config.getApiKey()
        AgentRunnerService(apiKey = apiKey) 
    }
}
