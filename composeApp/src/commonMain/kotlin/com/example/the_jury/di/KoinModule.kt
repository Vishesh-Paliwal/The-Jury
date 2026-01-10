package com.example.the_jury.di

import com.example.the_jury.Config
import com.example.the_jury.repo.PersonaRepository
import com.example.the_jury.service.AgentRunnerService
import com.example.the_jury.service.ChatPersistenceService
import com.example.the_jury.service.ChatPersistenceServiceImpl
import com.example.the_jury.service.JuryService
import com.example.the_jury.service.StreamingService
import com.example.the_jury.service.StreamingServiceImpl
import com.example.the_jury.service.TrialService
import com.example.the_jury.service.ModeratorAgent
import org.koin.dsl.module

val appModule = module {
    single { PersonaRepository() }
    
    // Persistence service (DatabaseDriverFactory is provided by platform modules)
    single<ChatPersistenceService> { ChatPersistenceServiceImpl(get()) }
    
    // Get API key from platform-specific Config
    single { 
        val apiKey = Config.getApiKey()
        AgentRunnerService(apiKey = apiKey) 
    }
    
    // Streaming service
    single<StreamingService> {
        val apiKey = Config.getApiKey()
        StreamingServiceImpl(apiKey = apiKey)
    }
    
    // Jury system services
    single { TrialService() }
    single { 
        val apiKey = Config.getApiKey()
        ModeratorAgent(apiKey = apiKey)
    }
    single { JuryService(get(), get(), get()) }
}
