import sys
import base64
import json
import os
from deepface import DeepFace
import numpy as np
from PIL import Image
import io

# Suppress TensorFlow warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

face_db = "face-recognition/db.json"

def decode_image_file(file_path, save_path):
    """Decode base64 image file and save as image"""
    try:
        with open(file_path, "r") as f:
            base64_str = f.read().strip()

        print(f"DEBUG: Base64 string length: {len(base64_str)}", file=sys.stderr)

        # Handle data URL format
        if base64_str.startswith('data:image'):
            base64_str = base64_str.split(',')[1]
            print("DEBUG: Removed data URL prefix", file=sys.stderr)

        image_data = base64.b64decode(base64_str)

        with open(save_path, "wb") as f:
            f.write(image_data)

        print(f"DEBUG: Image saved to {save_path}", file=sys.stderr)

        # Verify image can be opened
        with Image.open(save_path) as img:
            print(f"DEBUG: Image verified. Size: {img.size}, Mode: {img.mode}", file=sys.stderr)

    except Exception as e:
        print(f"DEBUG: Error decoding image: {str(e)}", file=sys.stderr)
        raise

def save_embedding(user_id, embedding):
    """Save user embedding to face database"""
    try:
        # Clean the user_id
        user_id = user_id.strip()

        if os.path.exists(face_db):
            with open(face_db, "r") as f:
                db = json.load(f)
            print(f"DEBUG: Loaded existing database with {len(db)} users", file=sys.stderr)
        else:
            db = {}
            print("DEBUG: Creating new face database", file=sys.stderr)

        # Check if user already exists
        if user_id in db:
            print(f"DEBUG: User [{user_id}] already exists, updating embedding", file=sys.stderr)
        else:
            print(f"DEBUG: Adding new user [{user_id}]", file=sys.stderr)

        db[user_id] = embedding

        with open(face_db, "w") as f:
            json.dump(db, f, indent=2)

        print(f"DEBUG: Database now contains {len(db)} users: {list(db.keys())}", file=sys.stderr)

    except Exception as e:
        print(f"DEBUG: Error saving embedding: {str(e)}", file=sys.stderr)
        raise

def debug_face_db():
    """Debug function to show current face database contents"""
    if os.path.exists(face_db):
        try:
            with open(face_db, "r") as f:
                db = json.load(f)
            print(f"DEBUG: Face DB contains {len(db)} users: {list(db.keys())}", file=sys.stderr)
            for username in db.keys():
                print(f"DEBUG: User key: [{username}] (length: {len(username)})", file=sys.stderr)
        except Exception as e:
            print(f"DEBUG: Error reading face DB: {str(e)}", file=sys.stderr)
    else:
        print("DEBUG: Face DB doesn't exist yet", file=sys.stderr)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("DEBUG: Invalid arguments. Usage: python register.py <base64_file_path> <user_id>", file=sys.stderr)
        sys.exit(1)

    base64_file_path = sys.argv[1]
    user_id = sys.argv[2].strip()  # Clean the user_id

    print(f"DEBUG: Registration started for user: [{user_id}]", file=sys.stderr)
    print(f"DEBUG: Base64 file path: {base64_file_path}", file=sys.stderr)

    # Show current DB state
    print("DEBUG: Current face database state:", file=sys.stderr)
    debug_face_db()

    try:
        # Ensure temp directory exists
        temp_dir = "face-recognition/temp"
        os.makedirs(temp_dir, exist_ok=True)

        image_path = os.path.join(temp_dir, "temp.jpg")

        # Decode and save image
        decode_image_file(base64_file_path, image_path)

        # Generate embedding
        print("DEBUG: Generating face embedding...", file=sys.stderr)
        result = DeepFace.represent(img_path=image_path, model_name="Facenet")
        embedding = result[0]["embedding"]

        print(f"DEBUG: Embedding generated successfully. Length: {len(embedding)}", file=sys.stderr)

        # Save embedding
        save_embedding(user_id, embedding)

        # Show final DB state
        print("DEBUG: Final face database state:", file=sys.stderr)
        debug_face_db()

        # Clean up temp file
        if os.path.exists(image_path):
            os.remove(image_path)
            print("DEBUG: Cleaned up temp image file", file=sys.stderr)

        print(f"Embedding saved for user: {user_id}")

    except Exception as e:
        print(f"DEBUG: Registration failed: {str(e)}", file=sys.stderr)
        print(f"Error during registration: {str(e)}")
        sys.exit(1)