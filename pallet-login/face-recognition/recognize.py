import sys
import os
import json
import numpy as np
from deepface import DeepFace
from numpy.linalg import norm
import cv2

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

if len(sys.argv) != 2:
    print("Unknown")
    sys.exit(1)

image_path = sys.argv[1]

if not os.path.exists(image_path):
    print("DEBUG: Image path doesn't exist", file=sys.stderr)
    print("Unknown")
    sys.exit(1)

img = cv2.imread(image_path)

if img is None:
    print("DEBUG: Failed to read image", file=sys.stderr)
    print("Unknown")
    sys.exit(1)

# Load face DB
db_path = "face-recognition/db.json"
if not os.path.exists(db_path):
    print("DEBUG: Face DB not found", file=sys.stderr)
    print("Unknown")
    sys.exit(1)

try:
    with open(db_path, "r") as f:
        db = json.load(f)
except Exception as e:
    print(f"DEBUG: Failed to load DB: {e}", file=sys.stderr)
    print("Unknown")
    sys.exit(1)

# Get embedding
try:
    input_embedding = DeepFace.represent(img_path=img, model_name="Facenet")[0]["embedding"]
except Exception as e:
    print(f"DEBUG: Failed to get embedding: {e}", file=sys.stderr)
    print("Unknown")
    sys.exit(1)

best_match = None
lowest_distance = float("inf")
threshold = 10

for username, embedding in db.items():
    try:
        distance = norm(np.array(input_embedding) - np.array(embedding))
        if distance < threshold and distance < lowest_distance:
            best_match = username
            lowest_distance = distance
    except:
        continue

if best_match:
    print(best_match.strip())
else:
    print("Unknown")
