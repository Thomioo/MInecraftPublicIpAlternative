import json
import os
import sys
import threading
from fastapi import FastAPI
from pyngrok import ngrok, conf
import uvicorn
from contextlib import asynccontextmanager

app = FastAPI()
"""======USER CONFIGURATION======"""
NGROK_STATIC_DOMAIN = ""  # Get your unique name from ngrok dashboard
conf.get_default().region = "eu"  # Change to your region if needed
NGROK_AUTH_TOKENS = [] # Add your ngrok auth tokens here
# Example: ["2cUf05GxcIexqEI2yWBP660P4ro_5w3V22ZfpbPRHgkC6nygZ", "2HjXlyPg3GzJK2nMFhc5ZMJJL0V_26xDUY26j82KgMw8E1dTg"]
"""=============================="""

public_address = None
tcp_tunnel = None
tcp_lock = threading.Lock()


current_token_index = 0

TOKEN_INDEX_FILE = "ngrok_token_index.json"

def save_token_index():
    with open(TOKEN_INDEX_FILE, "w") as f:
        json.dump({"current_token_index": current_token_index}, f)

def load_token_index():
    global current_token_index
    if os.path.exists(TOKEN_INDEX_FILE):
        with open(TOKEN_INDEX_FILE, "r") as f:
            data = json.load(f)
            current_token_index = data.get("current_token_index", 0)

def try_ngrok_connect_with_tokens(port):
    global tcp_tunnel
    token = NGROK_AUTH_TOKENS[current_token_index]
    try:
        ngrok.set_auth_token(token)
        tcp_tunnel = ngrok.connect(port, "tcp")
        print(f"Using token {token[:6]}... for TCP tunnel.")
        return tcp_tunnel.public_url
    except Exception as e:
        print(f"Token failed: {token[:6]}... - {e}")
        tcp_tunnel = None
        return None

def start_ngrok():
    global public_address
    with tcp_lock:
        public_address = try_ngrok_connect_with_tokens(25565)
        if public_address:
            print(f"Public address: {public_address.replace('tcp://', '')}")
        else:
            print("No available ngrok tokens for TCP tunnel.")

def switch_tcp_token():
    global current_token_index, public_address, tcp_tunnel
    with tcp_lock:
        current_token_index = (current_token_index + 1) % len(NGROK_AUTH_TOKENS)
        save_token_index()
        print(f"Switched to token: {NGROK_AUTH_TOKENS[current_token_index][:6]}... for TCP tunnel.")
        # Disconnect only the previous TCP tunnel
        if tcp_tunnel:
            ngrok.disconnect(tcp_tunnel.public_url)
            tcp_tunnel = None
        public_address = try_ngrok_connect_with_tokens(25565)
        if public_address:
            print(f"New public address: {public_address.replace('tcp://', '')}")
        else:
            print("No available ngrok tokens for TCP tunnel.")

def command_listener():
    while True:
        cmd = sys.stdin.readline().strip()
        if cmd == "/next":
            switch_tcp_token()
        elif cmd == "/exit":
            print("Exiting...")
            save_token_index()
            os._exit(0)

def setup_ngrok_tunnel():
    try:
        if not NGROK_STATIC_DOMAIN or "YOUR_UNIQUE_NAME" in NGROK_STATIC_DOMAIN:
            return None
        # Always use the first token for HTTP tunnel
        ngrok.set_auth_token(NGROK_AUTH_TOKENS[0])
        public_url = ngrok.connect(addr=8080, domain=NGROK_STATIC_DOMAIN).public_url
        print(f"Using token {NGROK_AUTH_TOKENS[0][:6]}... for HTTP tunnel.")
        return public_url
    except Exception as e:
        return None

@asynccontextmanager
async def lifespan(app: FastAPI):
    load_token_index()
    threading.Thread(target=command_listener, daemon=True).start()
    start_ngrok()
    yield

app = FastAPI(lifespan=lifespan)

@app.get("/ip")
async def get_ip():
    with tcp_lock:
        if public_address:
            return public_address.replace('tcp://', '')
        else:
            return "No TCP tunnel available."

if __name__ == "__main__":
    load_token_index()
    try:
        public_url = setup_ngrok_tunnel()
        if public_url:
            uvicorn.run(
                app,
                host="127.0.0.1",
                port=8080,
                log_level="info"
            )
    finally:
        save_token_index()