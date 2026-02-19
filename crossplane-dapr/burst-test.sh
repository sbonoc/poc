#!/bin/bash

# Configuration
URL="http://localhost:8080/publish"
TOTAL_REQUESTS=50

# 1. Start watching consumer logs in the background
echo "👀 Tailbox: Watching consumer logs..."
#kubectl logs -l app=consumer -c consumer -f &
#LOG_PID=$!

echo "🔥 Starting Burst Test: Sending $TOTAL_REQUESTS orders..."

# 2. Fire the requests
for i in $(seq 1 $TOTAL_REQUESTS)
do
   curl -s -X POST $URL \
     -H "Content-Type: application/json" \
     -d "{\"id\":\"ORD-$i\", \"amount\": $RANDOM.99}" > /dev/null &
   
   # Every 10 requests, give the event loop a tiny breath
   if [ $((i % 10)) -eq 0 ]; then
     wait
     echo "📤 Sent $i requests..."
   fi
done

wait
echo "✅ All requests sent. Waiting for consumer to finish processing..."
sleep 5

# 3. Cleanup log tail
#kill $LOG_PID
echo "🏁 Test Complete."