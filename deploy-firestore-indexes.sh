#!/bin/bash

# Deploy Firestore indexes
# This script deploys the Firestore indexes defined in firestore.indexes.json

echo "Deploying Firestore indexes..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null; then
    echo "Error: Firebase CLI is not installed."
    echo "Install it with: npm install -g firebase-tools"
    exit 1
fi

# Deploy indexes
firebase deploy --only firestore:indexes

echo "Firestore indexes deployment complete!"
echo ""
echo "Note: Index creation can take several minutes. You can check the status at:"
echo "https://console.firebase.google.com/project/financeapp-22b8b/firestore/indexes"
