import uvicorn, logging
from fastapi import FastAPI
from fastapi.responses import RedirectResponse
from pyngrok import ngrok, conf
from contextlib import asynccontextmanager
import os

"""======USER CONFIGURATION======"""
NGROK_STATIC_DOMAIN = "" # Get your unique name from ngrok dashboard
conf.get_default().region = "eu" # Change to your region if needed
IP_HISTORY_FILE = "ip_history.txt"
"""=============================="""

app = FastAPI()
public_address = None
logging.getLogger("pyngrok").setLevel(logging.CRITICAL)   # Suppress pyngrok logger

def store_ip_history(ip):
    if ip:
        with open(IP_HISTORY_FILE, "a") as f:
            f.write(ip + "\n")

def setup_ngrok_tunnel():
    try:
        if not NGROK_STATIC_DOMAIN or "YOUR_UNIQUE_NAME" in NGROK_STATIC_DOMAIN:
            return None

        public_url = ngrok.connect(addr=8080, domain=NGROK_STATIC_DOMAIN).public_url

        # Store the public_url in history
        store_ip_history(public_url)
        return public_url

    except Exception as e:
        return None

def start_ngrok():
    global public_address
    tunnel = ngrok.connect(25565, "tcp")
    public_address = tunnel.public_url
    # Store the public_address in history
    store_ip_history(public_address)

@asynccontextmanager
async def lifespan(app: FastAPI):
    start_ngrok()
    yield

app = FastAPI(lifespan=lifespan)

@app.get("/ip")
async def get_ip():
    return public_address.replace('tcp://', '')

if __name__ == "__main__":
    public_url = setup_ngrok_tunnel()
    print("Starting server...")
    
    if not public_url:
        print("Failed to create ngrok tunnel. Please check your configuration.")
    
    print(f"Public URL to put into the minecraft mod: {public_url}/ip")
    if public_url:
        uvicorn.run(
            app,          
            host="127.0.0.1", 
            port=8080,        
            log_level="info",
        )