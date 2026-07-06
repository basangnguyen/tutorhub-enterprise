package com.mycompany.tutorhub_enterprise.config;

public class AppConfig {
    // Sync Server
    public static final String SYNC_SERVER_URL = 
        System.getProperty("tutorhub.sync.url", "https://hocba299-3-tutorhub-sync.hf.space");
    
    // AI Server  
    public static final String AI_SERVER_URL = 
        System.getProperty("tutorhub.ai.url", "https://hocbatrolai293-tutorhub-ai.hf.space");
    
    // VSCode Server
    public static final String VSCODE_SERVER_URL = 
        System.getProperty("tutorhub.vscode.url", "https://hocbatrolai293-tutorhub-vscode.hf.space");
    
    // LiveKit  
    public static final String LIVEKIT_URL = 
        System.getProperty("tutorhub.livekit.url", "wss://tutorhub-enterprise-q820cqx7.livekit.cloud");
    
    // Auth Token
    public static final String API_AUTH_TOKEN = 
        System.getProperty("tutorhub.api.token", "TUTORHUB_SECRET_2026");
    
    // Core WebSocket
    public static final String CORE_WS_URL = 
        System.getProperty("tutorhub.core.ws", "wss://hocba299-3-tutorhub-core.hf.space");
}
