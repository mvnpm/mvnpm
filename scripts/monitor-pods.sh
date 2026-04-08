#!/bin/bash
# Monitor pods: memory usage and errors
# Usage: ./scripts/monitor-pods.sh [app_label] [duration_minutes] [interval_minutes]
# Defaults: app=mvnpm, 120 minutes duration, 10 minute intervals

APP=${1:-mvnpm}
DURATION_MIN=${2:-120}
INTERVAL_MIN=${3:-10}
END=$((SECONDS + DURATION_MIN * 60))
ITERATION=0

echo "Monitoring $APP pods for ${DURATION_MIN}m every ${INTERVAL_MIN}m..."
echo ""

while [ $SECONDS -lt $END ]; do
    ITERATION=$((ITERATION + 1))
    echo "=========================================="
    echo "CHECK #$ITERATION — $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=========================================="

    echo ""
    echo "--- POD STATUS & MEMORY ---"
    for pod in $(oc get pods -o name | grep "$APP" | grep -v postgres); do
        STATUS=$(oc get "$pod" -o jsonpath='{.status.phase}' 2>/dev/null)
        if [ "$STATUS" = "Running" ]; then
            MEM=$(oc exec "$pod" -- cat /sys/fs/cgroup/memory.current 2>/dev/null | tr -cd '0-9')
            LIMIT=$(oc exec "$pod" -- cat /sys/fs/cgroup/memory.max 2>/dev/null | tr -cd '0-9')
            if [ -n "$MEM" ] && [ "$MEM" -gt 0 ] 2>/dev/null; then
                MEM_MB=$((MEM / 1024 / 1024))
                LIMIT_MB=$((LIMIT / 1024 / 1024))
                POD_SHORT=$(echo "$pod" | sed "s|pod/${APP}-||")
                echo "  $POD_SHORT: ${MEM_MB}MB / ${LIMIT_MB}MB"
            fi
        fi
    done

    echo ""
    echo "--- RECENT ERRORS (last ${INTERVAL_MIN}m) ---"
    FOUND_ERRORS=0
    for pod in $(oc get pods -o name | grep "$APP" | grep -v postgres); do
        ERRORS=$(oc logs "$pod" --since="${INTERVAL_MIN}m" 2>/dev/null | grep -iE "ERROR|WARN|Exception|OOM|evict" | tail -5)
        if [ -n "$ERRORS" ]; then
            FOUND_ERRORS=1
            echo "$pod:"
            echo "$ERRORS"
            echo ""
        fi
    done
    if [ $FOUND_ERRORS -eq 0 ]; then
        echo "  (none)"
    fi

    echo ""
    echo ""

    if [ $SECONDS -lt $END ]; then
        sleep $((INTERVAL_MIN * 60))
    fi
done
echo "=========================================="
echo "MONITORING COMPLETE — $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="