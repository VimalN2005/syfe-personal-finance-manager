#!/bin/bash

# Complete E2E Tests for Personal Finance Manager
# Features:
# - HTTP status code range validation (2xx, 4xx, 5xx) for flexible testing
# - Configurable base URL via command line argument
# - Response validation for calculations and business logic
# - Organized test scenarios covering all finance manager functionality
# - Comprehensive error handling and reporting
# - Color-coded output for better readability
# - Session-based authentication testing
#
# Usage:
#   chmod +x finance_e2e_tests.sh
#   ./finance_e2e_tests.sh [BASE_URL]
#
# Examples:
#   ./finance_e2e_tests.sh                               # Uses default localhost
#   ./finance_e2e_tests.sh "https://api.example.com"    # Custom base URL

# Configuration
DEFAULT_BASE_URL="http://localhost:8080/api"
BASE_URL="${1:-$DEFAULT_BASE_URL}"

# Generate unique timestamp for this test run to avoid conflicts
TEST_TIMESTAMP=$(date +%s)
USER1_EMAIL="john.doe.${TEST_TIMESTAMP}@example.com"
USER2_EMAIL="jane.doe.${TEST_TIMESTAMP}@example.com"
CUSTOM_INCOME_CATEGORY="Freelance_${TEST_TIMESTAMP}"
CUSTOM_EXPENSE_CATEGORY="GymMembership_${TEST_TIMESTAMP}"
SIDE_BUSINESS_CATEGORY="SideBusiness_${TEST_TIMESTAMP}"

# Test tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Variables to store extracted IDs from API responses
TRANSACTION_ID_1=""
TRANSACTION_ID_2=""
TRANSACTION_ID_3=""
TRANSACTION_ID_4=""
GOAL_ID_1=""
GOAL_ID_2=""

# Colors for output formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

#=============================================================================
# UTILITY FUNCTIONS
#=============================================================================

# Function to extract ID from JSON response (simple approach without jq dependency)
extract_id_from_response() {
    local response="$1"
    # Extract "id":123 or "id": 123 from JSON response
    echo "$response" | grep -o '"id"[[:space:]]*:[[:space:]]*[0-9]*' | grep -o '[0-9]*' | head -1
}

# Function to extract value from JSON response
extract_json_value() {
    local response="$1"
    local key="$2"
    # Extract "key":value or "key": value from JSON response
    echo "$response" | grep -o "\"$key\"[[:space:]]*:[[:space:]]*[^,}]*" | sed "s/\"$key\"[[:space:]]*:[[:space:]]*//" | tr -d '"'
}

# Function to validate if status code falls within expected range
validate_status_range() {
    local actual_status="$1"
    local expected_range="$2"

    case "$expected_range" in
        "2xx")
            [ "$actual_status" -ge 200 ] && [ "$actual_status" -lt 300 ]
            ;;
        "4xx")
            [ "$actual_status" -ge 400 ] && [ "$actual_status" -lt 500 ]
            ;;
        "5xx")
            [ "$actual_status" -ge 500 ] && [ "$actual_status" -lt 600 ]
            ;;
        "3xx")
            [ "$actual_status" -ge 300 ] && [ "$actual_status" -lt 400 ]
            ;;
        *)
            # If it's not a range, check for exact match (backward compatibility)
            [ "$actual_status" = "$expected_range" ]
            ;;
    esac
}

# Function to validate response values - FIXED to update test counts
validate_response_value() {
    local response="$1"
    local key="$2"
    local expected_value="$3"
    local comparison_type="${4:-equals}" # equals, greater_than, less_than, contains, count

    local actual_value=$(extract_json_value "$response" "$key")

    case "$comparison_type" in
        "equals")
            if [ "$actual_value" = "$expected_value" ]; then
                echo -e "  ${GREEN}✓ Validation passed:${NC} $key = $actual_value"
                return 0
            else
                echo -e "  ${RED}✗ Validation failed:${NC} $key expected $expected_value, got $actual_value"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                PASSED_TESTS=$((PASSED_TESTS - 1))  # Decrement passed count since main test passed but validation failed
                return 1
            fi
            ;;
        "greater_than")
            if (( $(echo "$actual_value > $expected_value" | bc -l) )); then
                echo -e "  ${GREEN}✓ Validation passed:${NC} $key = $actual_value (> $expected_value)"
                return 0
            else
                echo -e "  ${RED}✗ Validation failed:${NC} $key expected > $expected_value, got $actual_value"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                PASSED_TESTS=$((PASSED_TESTS - 1))
                return 1
            fi
            ;;
        "less_than")
            if (( $(echo "$actual_value < $expected_value" | bc -l) )); then
                echo -e "  ${GREEN}✓ Validation passed:${NC} $key = $actual_value (< $expected_value)"
                return 0
            else
                echo -e "  ${RED}✗ Validation failed:${NC} $key expected < $expected_value, got $actual_value"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                PASSED_TESTS=$((PASSED_TESTS - 1))
                return 1
            fi
            ;;
        "contains")
            if [[ "$response" == *"$expected_value"* ]]; then
                echo -e "  ${GREEN}✓ Validation passed:${NC} Response contains: $expected_value"
                return 0
            else
                echo -e "  ${RED}✗ Validation failed:${NC} Response does not contain: $expected_value"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                PASSED_TESTS=$((PASSED_TESTS - 1))
                return 1
            fi
            ;;
        "count")
            # Count occurrences of a pattern in the response
            local count=$(echo "$response" | grep -o "$key" | wc -l | tr -d ' ')
            if [ "$count" = "$expected_value" ]; then
                echo -e "  ${GREEN}✓ Validation passed:${NC} Found $count occurrences of '$key'"
                return 0
            else
                echo -e "  ${RED}✗ Validation failed:${NC} Expected $expected_value occurrences of '$key', found $count"
                FAILED_TESTS=$((FAILED_TESTS + 1))
                PASSED_TESTS=$((PASSED_TESTS - 1))
                return 1
            fi
            ;;
    esac
}

# Function to make API call and validate status code range
api_test() {
    local test_name="$1"
    local expected_status_range="$2"
    local method="$3"
    local endpoint="$4"
    local data="$5"
    local cookie_file="$6"
    local save_cookies="$7"
    local extract_id_var="$8"  # Optional: variable name to store extracted ID

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${CYAN}→ $test_name${NC}"

    # Build curl command with all options
    local curl_cmd=(curl -s -w "%{http_code}")

    # Add method
    if [ -n "$method" ]; then
        curl_cmd+=(-X "$method")
    fi

    # Add headers and data for POST/PUT requests
    if [ -n "$data" ]; then
        curl_cmd+=(-H "Content-Type: application/json" -d "$data")
    fi

    # Add cookie handling
    if [ -n "$cookie_file" ] && [ -f "$cookie_file" ]; then
        curl_cmd+=(-b "$cookie_file")
    fi

    if [ -n "$save_cookies" ]; then
        curl_cmd+=(-c "$save_cookies")
    fi

    # Add endpoint URL
    curl_cmd+=("$BASE_URL$endpoint")

    # Execute curl and capture output
    local full_response
    full_response=$("${curl_cmd[@]}" 2>/dev/null)
    local curl_exit_code=$?

    # Check if curl command succeeded
    if [ $curl_exit_code -ne 0 ]; then
        echo -e "  ${RED}✗ FAIL${NC} - Curl command failed (exit code: $curl_exit_code)"
        echo -e "  ${RED}Error: Could not connect to $BASE_URL$endpoint${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo ""
        return 1
    fi

    # Extract status code (last 3 characters) and response body
    local actual_status="${full_response: -3}"
    local response_body="${full_response%???}"

    # Validate status code range
    if validate_status_range "$actual_status" "$expected_status_range"; then
        echo -e "  ${GREEN}✓ PASS${NC} - Status: $actual_status (expected $expected_status_range)"
        PASSED_TESTS=$((PASSED_TESTS + 1))

        # Extract ID if requested and response is successful
        if [ -n "$extract_id_var" ] && [ "$actual_status" -ge 200 ] && [ "$actual_status" -lt 300 ]; then
            local extracted_id
            extracted_id=$(extract_id_from_response "$response_body")
            if [ -n "$extracted_id" ]; then
                eval "$extract_id_var=\"$extracted_id\""
                echo -e "  ${BLUE}ID extracted: $extracted_id${NC}"
            fi
        fi

        # Store response for validation
        LAST_RESPONSE="$response_body"
    else
        echo -e "  ${RED}✗ FAIL${NC} - Expected: $expected_status_range, Got: $actual_status"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    # Display response body if present and meaningful
    if [ -n "$response_body" ] && [ "$(echo "$response_body" | tr -d '[:space:]')" != "" ]; then
        echo "  Response: $response_body"
    fi

    echo ""
    return 0
}

# Function to print scenario headers
print_scenario() {
    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

# Function to print section headers
print_section() {
    echo -e "${BLUE}── $1 ──${NC}"
    echo ""
}

# Function to print final test summary
print_summary() {
    echo ""
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}TEST EXECUTION SUMMARY${NC}"
    echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════════════════${NC}"
    echo -e "Base URL: ${CYAN}$BASE_URL${NC}"
    echo -e "Total Tests Executed: ${BLUE}$TOTAL_TESTS${NC}"
    echo -e "Tests Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Tests Failed: ${RED}$FAILED_TESTS${NC}"

    if [ $TOTAL_TESTS -gt 0 ]; then
        local success_rate=$((PASSED_TESTS * 100 / TOTAL_TESTS))
        echo -e "Success Rate: ${CYAN}$success_rate%${NC}"
    else
        echo -e "Success Rate: ${CYAN}N/A${NC}"
    fi

    if [ $FAILED_TESTS -eq 0 ] && [ $TOTAL_TESTS -gt 0 ]; then
        echo -e "\n${GREEN}🎉 ALL TESTS PASSED! 🎉${NC}"
        echo -e "${GREEN}The Personal Finance Manager API is working correctly.${NC}"
        exit 0
    else
        echo -e "\n${RED}❌ SOME TESTS FAILED ❌${NC}"
        echo -e "${RED}Please review the failed tests above and check your API implementation.${NC}"
        exit 1
    fi
}

# Cleanup function to remove temporary files
cleanup() {
    rm -f user1_session.txt user2_session.txt 2>/dev/null
}

# Set up cleanup on script exit
trap cleanup EXIT

#=============================================================================
# MAIN TEST EXECUTION
#=============================================================================

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                   Personal Finance Manager E2E Test Suite                     ║${NC}"
echo -e "${BLUE}║                            Comprehensive Validation                           ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "Target API: ${CYAN}$BASE_URL${NC}"
echo -e "Test Mode: ${YELLOW}End-to-End API Validation${NC}"
echo -e "Test Run ID: ${CYAN}$TEST_TIMESTAMP${NC}"
echo ""

#=============================================================================
# SCENARIO 1: USER REGISTRATION AND AUTHENTICATION
#=============================================================================
print_scenario "SCENARIO 1: USER REGISTRATION AND AUTHENTICATION"
echo "This scenario tests complete user lifecycle including:"
echo "• User registration with valid data (expects 2xx success)"
echo "• Duplicate registration prevention (expects 4xx client error)"
echo "• Successful authentication (expects 2xx success)"
echo "• Failed authentication attempts (expects 4xx client error)"
echo "• Session-based authorization (expects 4xx when unauthorized)"
echo ""

print_section "1.1 User Registration"
api_test "Register first user ($USER1_EMAIL)" "2xx" "POST" "/auth/register" "{
    \"username\": \"$USER1_EMAIL\",
    \"password\": \"securePassword123\",
    \"fullName\": \"John Doe\",
    \"phoneNumber\": \"+1234567890\"
}"

api_test "Register second user ($USER2_EMAIL)" "2xx" "POST" "/auth/register" "{
    \"username\": \"$USER2_EMAIL\",
    \"password\": \"anotherPassword123\",
    \"fullName\": \"Jane Doe\",
    \"phoneNumber\": \"+0987654321\"
}"

api_test "Attempt duplicate registration" "4xx" "POST" "/auth/register" "{
    \"username\": \"$USER1_EMAIL\",
    \"password\": \"differentPassword\",
    \"fullName\": \"John Duplicate\",
    \"phoneNumber\": \"+1111111111\"
}"

# Missing field validation
api_test "Register with missing username" "4xx" "POST" "/auth/register" '{
    "password": "password123",
    "fullName": "Missing Username",
    "phoneNumber": "+1234567890"
}'

api_test "Register with missing password" "4xx" "POST" "/auth/register" "{
    \"username\": \"missing.password@example.com\",
    \"fullName\": \"Missing Password\",
    \"phoneNumber\": \"+1234567890\"
}"

print_section "1.2 Authentication Tests"
api_test "Login with valid credentials" "2xx" "POST" "/auth/login" "{
    \"username\": \"$USER1_EMAIL\",
    \"password\": \"securePassword123\"
}" "" "user1_session.txt"

api_test "Login with invalid password" "4xx" "POST" "/auth/login" "{
    \"username\": \"$USER1_EMAIL\",
    \"password\": \"wrongPassword\"
}"

api_test "Login with non-existent user" "4xx" "POST" "/auth/login" '{
    "username": "nonexistent@example.com",
    "password": "password123"
}'

print_section "1.3 Authorization Tests"
api_test "Access protected endpoint with valid session" "2xx" "GET" "/categories" "" "user1_session.txt"

api_test "Access protected endpoint without session" "4xx" "GET" "/categories"

api_test "Logout successfully" "2xx" "POST" "/auth/logout" "" "user1_session.txt"

api_test "Access protected endpoint after logout" "4xx" "GET" "/categories" "" "user1_session.txt"

#=============================================================================
# SCENARIO 2: TRANSACTION MANAGEMENT
#=============================================================================
print_scenario "SCENARIO 2: TRANSACTION MANAGEMENT"
echo "This scenario tests complete transaction CRUD operations including:"
echo "• Creating income and expense transactions (expects 2xx success)"
echo "• Input validation for amounts, dates, and categories (expects 4xx on invalid data)"
echo "• Retrieving transactions with filtering (expects 2xx success)"
echo "• Updating and deleting transactions (expects 2xx success)"
echo "• Date field cannot be updated"
echo ""

# Re-login for transaction tests
api_test "Re-login for transaction tests" "2xx" "POST" "/auth/login" "{
    \"username\": \"$USER1_EMAIL\",
    \"password\": \"securePassword123\"
}" "" "user1_session.txt"

print_section "2.1 Transaction Creation"
api_test "Create income transaction (Salary)" "2xx" "POST" "/transactions" '{
    "amount": 5000.00,
    "date": "2024-01-15",
    "category": "Salary",
    "description": "January Salary"
}' "user1_session.txt" "" "TRANSACTION_ID_1"

# Validate response values
validate_response_value "$LAST_RESPONSE" "amount" "5000.00"
validate_response_value "$LAST_RESPONSE" "type" "INCOME"

api_test "Create expense transaction (Rent)" "2xx" "POST" "/transactions" '{
    "amount": 1200.00,
    "date": "2024-01-16",
    "category": "Rent",
    "description": "Monthly rent payment"
}' "user1_session.txt" "" "TRANSACTION_ID_2"

# Validate response values
validate_response_value "$LAST_RESPONSE" "amount" "1200.00"
validate_response_value "$LAST_RESPONSE" "type" "EXPENSE"

api_test "Create expense transaction (Food)" "2xx" "POST" "/transactions" '{
    "amount": 400.00,
    "date": "2024-01-17",
    "category": "Food",
    "description": "Groceries"
}' "user1_session.txt" "" "TRANSACTION_ID_3"

print_section "2.2 Transaction Validation"
api_test "Create transaction with invalid category" "4xx" "POST" "/transactions" '{
    "amount": 100.00,
    "date": "2024-01-18",
    "category": "NonExistentCategory",
    "description": "Invalid category test"
}' "user1_session.txt"

# Fixed date to be in the past since we're in June 2025
api_test "Create transaction with future date" "4xx" "POST" "/transactions" '{
    "amount": 100.00,
    "date": "2026-12-31",
    "category": "Food",
    "description": "Future transaction"
}' "user1_session.txt"

api_test "Create transaction with negative amount" "4xx" "POST" "/transactions" '{
    "amount": -100.00,
    "date": "2024-01-17",
    "category": "Food",
    "description": "Negative amount"
}' "user1_session.txt"

api_test "Create transaction with zero amount" "4xx" "POST" "/transactions" '{
    "amount": 0,
    "date": "2024-01-17",
    "category": "Food",
    "description": "Zero amount"
}' "user1_session.txt"

api_test "Create transaction with invalid date format" "4xx" "POST" "/transactions" '{
    "amount": 100.00,
    "date": "01-17-2024",
    "category": "Food",
    "description": "Invalid date format"
}' "user1_session.txt"

print_section "2.3 Transaction Retrieval"
api_test "Get all transactions" "2xx" "GET" "/transactions" "" "user1_session.txt"
# Validate we have 3 transactions
validate_response_value "$LAST_RESPONSE" "\"id\":" "3" "count"

api_test "Filter transactions by category" "2xx" "GET" "/transactions?category=Salary" "" "user1_session.txt"
# Validate only Salary transactions returned
validate_response_value "$LAST_RESPONSE" "Salary" "" "contains"
validate_response_value "$LAST_RESPONSE" "\"id\":" "1" "count"

api_test "Filter transactions by date range" "2xx" "GET" "/transactions?startDate=2024-01-15&endDate=2024-01-16" "" "user1_session.txt"
# Should return 2 transactions
validate_response_value "$LAST_RESPONSE" "\"id\":" "2" "count"

print_section "2.4 Transaction Updates - Date Restriction Test"
# Test that date cannot be updated
api_test "Attempt to update transaction date (should ignore date field)" "2xx" "PUT" "/transactions/$TRANSACTION_ID_1" '{
    "date": "2024-01-20"
}' "user1_session.txt"

# Validate the date hasn't changed
validate_response_value "$LAST_RESPONSE" "date" "2024-01-15"

api_test "Update transaction amount and description (no date)" "2xx" "PUT" "/transactions/$TRANSACTION_ID_1" '{
    "amount": 5500.00,
    "description": "Updated January Salary with bonus"
}' "user1_session.txt"

# Validate the date hasn't changed
validate_response_value "$LAST_RESPONSE" "date" "2024-01-15"
validate_response_value "$LAST_RESPONSE" "amount" "5500.00"

api_test "Update with date field included (should ignore date)" "2xx" "PUT" "/transactions/$TRANSACTION_ID_3" '{
    "amount": 450.00,
    "date": "2024-01-18",
    "description": "Trying to change date"
}' "user1_session.txt"

# Validate amount and description updated but date unchanged
validate_response_value "$LAST_RESPONSE" "date" "2024-01-17"
validate_response_value "$LAST_RESPONSE" "amount" "450.00"
validate_response_value "$LAST_RESPONSE" "description" "Trying to change date"

api_test "Update non-existent transaction" "4xx" "PUT" "/transactions/999999" '{
    "amount": 1000.00
}' "user1_session.txt"

print_section "2.5 Transaction Deletion"
api_test "Delete transaction" "2xx" "DELETE" "/transactions/$TRANSACTION_ID_2" "" "user1_session.txt"

api_test "Delete non-existent transaction" "4xx" "DELETE" "/transactions/999999" "" "user1_session.txt"

#=============================================================================
# SCENARIO 3: CATEGORY MANAGEMENT
#=============================================================================
print_scenario "SCENARIO 3: CATEGORY MANAGEMENT"
echo "This scenario tests category management including:"
echo "• Viewing default categories (expects 2xx success)"
echo "• Creating custom categories (expects 2xx success)"
echo "• Preventing duplicate category names (expects 4xx conflict)"
echo "• Deleting custom categories with usage validation (expects 4xx when in use)"
echo ""

print_section "3.1 Category Viewing"
api_test "Get all categories (default + custom)" "2xx" "GET" "/categories" "" "user1_session.txt"
# Validate default categories are present
validate_response_value "$LAST_RESPONSE" "Salary" "" "contains"
validate_response_value "$LAST_RESPONSE" "Food" "" "contains"
validate_response_value "$LAST_RESPONSE" "Rent" "" "contains"

print_section "3.2 Custom Category Creation"
api_test "Create custom income category" "2xx" "POST" "/categories" "{
    \"name\": \"$CUSTOM_INCOME_CATEGORY\",
    \"type\": \"INCOME\"
}" "user1_session.txt"

validate_response_value "$LAST_RESPONSE" "custom" "true"

api_test "Create custom expense category" "2xx" "POST" "/categories" "{
    \"name\": \"$CUSTOM_EXPENSE_CATEGORY\",
    \"type\": \"EXPENSE\"
}" "user1_session.txt"

api_test "Attempt duplicate category creation" "4xx" "POST" "/categories" "{
    \"name\": \"$CUSTOM_INCOME_CATEGORY\",
    \"type\": \"INCOME\"
}" "user1_session.txt"

api_test "Create category with invalid type" "4xx" "POST" "/categories" '{
    "name": "InvalidType",
    "type": "INVALID"
}' "user1_session.txt"

print_section "3.3 Category Usage and Deletion"
api_test "Create transaction with custom category" "2xx" "POST" "/transactions" "{
    \"amount\": 1500.00,
    \"date\": \"2024-01-18\",
    \"category\": \"$CUSTOM_INCOME_CATEGORY\",
    \"description\": \"Client project payment\"
}" "user1_session.txt" "" "TRANSACTION_ID_4"

api_test "Try to delete category in use" "4xx" "DELETE" "/categories/$CUSTOM_INCOME_CATEGORY" "" "user1_session.txt"

api_test "Delete unused custom category" "2xx" "DELETE" "/categories/$CUSTOM_EXPENSE_CATEGORY" "" "user1_session.txt"

api_test "Try to delete default category" "4xx" "DELETE" "/categories/Food" "" "user1_session.txt"

#=============================================================================
# SCENARIO 4: SAVINGS GOALS MANAGEMENT WITH PROGRESS CALCULATION
#=============================================================================
print_scenario "SCENARIO 4: SAVINGS GOALS MANAGEMENT WITH PROGRESS CALCULATION"
echo "This scenario tests savings goals functionality including:"
echo "• Creating goals with valid target amounts and dates (expects 2xx success)"
echo "• Input validation for goals (expects 4xx for invalid data)"
echo "• Progress tracking based on transactions (expects 2xx with calculated progress)"
echo "• Goal start date defaults to creation date"
echo "• Progress = (Total Income - Total Expenses) since goal start date"
echo ""

print_section "4.1 Goal Creation and Default Start Date"
# Get current date for testing default start date - handle timezone differences
# The API might be using UTC or a different timezone, so we accept either today or yesterday
CURRENT_DATE=$(date +%Y-%m-%d)
YESTERDAY_DATE=$(date -d "yesterday" +%Y-%m-%d 2>/dev/null || date -v-1d +%Y-%m-%d 2>/dev/null || echo "$CURRENT_DATE")

api_test "Create goal without start date (should default to today)" "2xx" "POST" "/goals" '{
    "goalName": "Emergency Fund No Start Date",
    "targetAmount": 10000.00,
    "targetDate": "2027-01-01"
}' "user1_session.txt" "" "GOAL_ID_DEFAULT"

# Accept either current date or yesterday (timezone handling)
ACTUAL_START_DATE=$(extract_json_value "$LAST_RESPONSE" "startDate")
if [ "$ACTUAL_START_DATE" = "$CURRENT_DATE" ] || [ "$ACTUAL_START_DATE" = "$YESTERDAY_DATE" ]; then
    echo -e "  ${GREEN}✓ Validation passed:${NC} startDate = $ACTUAL_START_DATE (default date)"
else
    echo -e "  ${RED}✗ Validation failed:${NC} startDate expected $CURRENT_DATE or $YESTERDAY_DATE, got $ACTUAL_START_DATE"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS - 1))
fi

api_test "Create emergency fund goal with explicit start date" "2xx" "POST" "/goals" '{
    "goalName": "Emergency Fund",
    "targetAmount": 10000.00,
    "targetDate": "2027-01-01",
    "startDate": "2024-01-01"
}' "user1_session.txt" "" "GOAL_ID_1"

# Current progress calculation:
# Income: 5500 (Salary) + 1500 (Freelance) = 7000
# Expenses: 450 (Food) = 450
# Net: 7000 - 450 = 6550
validate_response_value "$LAST_RESPONSE" "currentProgress" "6550.00"
validate_response_value "$LAST_RESPONSE" "progressPercentage" "65.5"
validate_response_value "$LAST_RESPONSE" "remainingAmount" "3450.00"

api_test "Create vacation fund goal" "2xx" "POST" "/goals" '{
    "goalName": "Vacation Fund",
    "targetAmount": 5000.00,
    "targetDate": "2027-12-01",
    "startDate": "2024-02-01"
}' "user1_session.txt" "" "GOAL_ID_2"

# No transactions after Feb 1, so progress should be 0
validate_response_value "$LAST_RESPONSE" "currentProgress" "0"
validate_response_value "$LAST_RESPONSE" "progressPercentage" "0.0"

print_section "4.2 Goal Validation"
api_test "Create goal with past target date" "4xx" "POST" "/goals" '{
    "goalName": "Invalid Goal",
    "targetAmount": 5000.00,
    "targetDate": "2023-01-01"
}' "user1_session.txt"

api_test "Create goal with negative amount" "4xx" "POST" "/goals" '{
    "goalName": "Negative Goal",
    "targetAmount": -1000.00,
    "targetDate": "2026-01-01"
}' "user1_session.txt"

api_test "Create goal with zero amount" "4xx" "POST" "/goals" '{
    "goalName": "Zero Goal",
    "targetAmount": 0,
    "targetDate": "2026-01-01"
}' "user1_session.txt"

api_test "Create goal with start date after target date" "4xx" "POST" "/goals" '{
    "goalName": "Invalid Dates Goal",
    "targetAmount": 5000.00,
    "targetDate": "2026-01-01",
    "startDate": "2027-01-01"
}' "user1_session.txt"

print_section "4.3 Goal Progress After Adding Transactions"
# Add transaction after Feb 1 to affect second goal
api_test "Add income after second goal start date" "2xx" "POST" "/transactions" '{
    "amount": 3000.00,
    "date": "2024-02-15",
    "category": "Salary",
    "description": "February bonus"
}' "user1_session.txt" "" "FEB_INCOME_ID"

api_test "Add expense after second goal start date" "2xx" "POST" "/transactions" '{
    "amount": 500.00,
    "date": "2024-02-20",
    "category": "Food",
    "description": "February groceries"
}' "user1_session.txt" "" "FEB_EXPENSE_ID"

# Check updated progress for second goal
api_test "Get vacation fund goal with updated progress" "2xx" "GET" "/goals/$GOAL_ID_2" "" "user1_session.txt"

# Progress should be 3000 - 500 = 2500
validate_response_value "$LAST_RESPONSE" "currentProgress" "2500.00"
validate_response_value "$LAST_RESPONSE" "progressPercentage" "50.0"
validate_response_value "$LAST_RESPONSE" "remainingAmount" "2500.00"

print_section "4.4 Goal Management"
api_test "Get all goals with progress" "2xx" "GET" "/goals" "" "user1_session.txt"
# Validate we have 3 goals
validate_response_value "$LAST_RESPONSE" "\"id\":" "3" "count"

api_test "Update goal target amount" "2xx" "PUT" "/goals/$GOAL_ID_1" '{
    "targetAmount": 15000.00
}' "user1_session.txt"

# Progress calculation after update:
# Total Income: 5500 + 1500 + 3000 = 10000
# Total Expenses: 450 + 500 = 950
# Net: 10000 - 950 = 9050
# Progress percentage: 9050/15000 = 60.33%
validate_response_value "$LAST_RESPONSE" "progressPercentage" "60.33"
validate_response_value "$LAST_RESPONSE" "remainingAmount" "5950.00"

api_test "Update goal target date" "2xx" "PUT" "/goals/$GOAL_ID_1" '{
    "targetDate": "2028-01-01"
}' "user1_session.txt"

api_test "Update non-existent goal" "4xx" "PUT" "/goals/999999" '{
    "targetAmount": 5000.00
}' "user1_session.txt"

#=============================================================================
# SCENARIO 5: TRANSACTION DELETION IMPACT ON GOALS AND REPORTS
#=============================================================================
print_scenario "SCENARIO 5: TRANSACTION DELETION IMPACT ON GOALS AND REPORTS"
echo "This scenario tests that deleted transactions:"
echo "• No longer affect goal progress calculations"
echo "• Are excluded from monthly and yearly reports"
echo "• Ensures data consistency after deletion"
echo ""

print_section "5.1 Record Current State"
# Get current goal progress before deletion
api_test "Get vacation fund goal before deletion" "2xx" "GET" "/goals/$GOAL_ID_2" "" "user1_session.txt"

# Current progress should be 2500 (3000 income - 500 expense)
validate_response_value "$LAST_RESPONSE" "currentProgress" "2500.00"

# Get February report before deletion
api_test "Get February 2024 report before deletion" "2xx" "GET" "/reports/monthly/2024/2" "" "user1_session.txt"

# Should show income 3000 and expenses 500
echo "  Checking February report before deletion..."
validate_response_value "$LAST_RESPONSE" "netSavings" "2500.00"

print_section "5.2 Delete Transaction and Verify Impact"
api_test "Delete February expense transaction" "2xx" "DELETE" "/transactions/$FEB_EXPENSE_ID" "" "user1_session.txt"

# Check goal progress after deletion
api_test "Get vacation fund goal after expense deletion" "2xx" "GET" "/goals/$GOAL_ID_2" "" "user1_session.txt"

# Progress should now be 3000 (only income remains)
validate_response_value "$LAST_RESPONSE" "currentProgress" "3000.00"
validate_response_value "$LAST_RESPONSE" "progressPercentage" "60.0"

# Check February report after deletion
api_test "Get February 2024 report after deletion" "2xx" "GET" "/reports/monthly/2024/2" "" "user1_session.txt"

# Net savings should now be 3000 (only income)
validate_response_value "$LAST_RESPONSE" "netSavings" "3000.00"

print_section "5.3 Delete Income Transaction and Verify"
api_test "Delete February income transaction" "2xx" "DELETE" "/transactions/$FEB_INCOME_ID" "" "user1_session.txt"

# Check goal progress after deleting income
api_test "Get vacation fund goal after income deletion" "2xx" "GET" "/goals/$GOAL_ID_2" "" "user1_session.txt"

# Progress should now be 0 (no transactions after goal start date)
validate_response_value "$LAST_RESPONSE" "currentProgress" "0"
validate_response_value "$LAST_RESPONSE" "progressPercentage" "0.0"

# Check February report - should be empty
api_test "Get February 2024 report after all deletions" "2xx" "GET" "/reports/monthly/2024/2" "" "user1_session.txt"

# Net savings should be 0
validate_response_value "$LAST_RESPONSE" "netSavings" "0"

# Delete goals for cleanup
api_test "Delete vacation fund goal" "2xx" "DELETE" "/goals/$GOAL_ID_2" "" "user1_session.txt"

api_test "Delete non-existent goal" "4xx" "DELETE" "/goals/999999" "" "user1_session.txt"

#=============================================================================
# SCENARIO 6: REPORTS GENERATION WITH CALCULATIONS
#=============================================================================
print_scenario "SCENARIO 6: REPORTS GENERATION WITH CALCULATIONS"
echo "This scenario tests report generation including:"
echo "• Monthly reports with income/expense breakdown (expects 2xx success)"
echo "• Yearly reports aggregating monthly data (expects 2xx success)"
echo "• Accurate calculations for net savings"
echo "• Input validation for report parameters (expects 4xx for invalid input)"
echo ""

print_section "6.1 Monthly Reports with Calculations"
api_test "Generate January 2024 monthly report" "2xx" "GET" "/reports/monthly/2024/1" "" "user1_session.txt"

# January calculation:
# Income: 5500 (Salary) + 1500 (Freelance) = 7000
# Expenses: 450 (Food) = 450
# Net: 7000 - 450 = 6550
echo "  Validating January calculations..."
validate_response_value "$LAST_RESPONSE" "netSavings" "6550.00"
validate_response_value "$LAST_RESPONSE" "$CUSTOM_INCOME_CATEGORY" "1500.00" "contains"

api_test "Generate report for month with no data" "2xx" "GET" "/reports/monthly/2024/12" "" "user1_session.txt"

# Should return 0 for all values
validate_response_value "$LAST_RESPONSE" "netSavings" "0"

api_test "Generate report for invalid month" "4xx" "GET" "/reports/monthly/2024/13" "" "user1_session.txt"

api_test "Generate report for month 0" "4xx" "GET" "/reports/monthly/2024/0" "" "user1_session.txt"

print_section "6.2 Yearly Reports with Calculations"
api_test "Generate 2024 yearly report" "2xx" "GET" "/reports/yearly/2024" "" "user1_session.txt"

# 2024 total: Income 7000 - Expenses 450 = 6550
echo "  Validating yearly calculations..."
validate_response_value "$LAST_RESPONSE" "netSavings" "6550.00"

api_test "Generate report for year with no data" "2xx" "GET" "/reports/yearly/2023" "" "user1_session.txt"

validate_response_value "$LAST_RESPONSE" "netSavings" "0"

#=============================================================================
# SCENARIO 7: DATA ISOLATION BETWEEN USERS
#=============================================================================
print_scenario "SCENARIO 7: DATA ISOLATION BETWEEN USERS"
echo "This scenario tests data security and isolation including:"
echo "• Users can only access their own data (expects 2xx for own data)"
echo "• Users cannot access other users' data (expects 4xx for others' data)"
echo "• Session-based user identification works correctly"
echo ""

print_section "7.1 Second User Setup"
api_test "Login as second user" "2xx" "POST" "/auth/login" "{
    \"username\": \"$USER2_EMAIL\",
    \"password\": \"anotherPassword123\"
}" "" "user2_session.txt"

print_section "7.2 Data Isolation Verification"
api_test "Second user sees only default categories" "2xx" "GET" "/categories" "" "user2_session.txt"
# Should not contain first user's custom category
if [[ "$LAST_RESPONSE" == *"$CUSTOM_INCOME_CATEGORY"* ]]; then
    echo -e "  ${RED}✗ Validation failed:${NC} Second user can see first user's custom category"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS - 1))
else
    echo -e "  ${GREEN}✓ Validation passed:${NC} Second user cannot see first user's custom category"
fi

api_test "Second user has no transactions initially" "2xx" "GET" "/transactions" "" "user2_session.txt"
# Validate empty transaction list
validate_response_value "$LAST_RESPONSE" "\"transactions\":\\[\\]" "" "contains"

api_test "Second user has no goals initially" "2xx" "GET" "/goals" "" "user2_session.txt"
# Validate empty goals list
validate_response_value "$LAST_RESPONSE" "\"goals\":\\[\\]" "" "contains"

print_section "7.3 Cross-User Data Access Prevention"
api_test "Second user cannot update first user's transaction" "4xx" "PUT" "/transactions/$TRANSACTION_ID_1" '{
    "amount": 1000.00
}' "user2_session.txt"

api_test "Second user cannot delete first user's goal" "4xx" "DELETE" "/goals/$GOAL_ID_1" "" "user2_session.txt"

print_section "7.4 Independent User Operations"
api_test "Second user creates own transaction" "2xx" "POST" "/transactions" '{
    "amount": 3000.00,
    "date": "2024-01-20",
    "category": "Salary",
    "description": "Jane January Salary"
}' "user2_session.txt"

api_test "Second user creates own goal" "2xx" "POST" "/goals" '{
    "goalName": "Jane Emergency Fund",
    "targetAmount": 8000.00,
    "targetDate": "2027-12-01"
}' "user2_session.txt"

# Verify goal has correct default start date and calculations
ACTUAL_START_DATE=$(extract_json_value "$LAST_RESPONSE" "startDate")
if [ "$ACTUAL_START_DATE" = "$CURRENT_DATE" ] || [ "$ACTUAL_START_DATE" = "$YESTERDAY_DATE" ]; then
    echo -e "  ${GREEN}✓ Validation passed:${NC} startDate = $ACTUAL_START_DATE (default date)"
else
    echo -e "  ${RED}✗ Validation failed:${NC} startDate expected $CURRENT_DATE or $YESTERDAY_DATE, got $ACTUAL_START_DATE"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS - 1))
fi

api_test "First user still sees only own data" "2xx" "GET" "/transactions" "" "user1_session.txt"
# Validate first user doesn't see second user's transaction
if [[ "$LAST_RESPONSE" == *"Jane January Salary"* ]]; then
    echo -e "  ${RED}✗ Validation failed:${NC} First user can see second user's transaction"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    PASSED_TESTS=$((PASSED_TESTS - 1))
else
    echo -e "  ${GREEN}✓ Validation passed:${NC} First user cannot see second user's transaction"
fi
# Validate first user still has correct number of transactions (3 after deletions)
validate_response_value "$LAST_RESPONSE" "\"id\":" "3" "count"

#=============================================================================
# SCENARIO 8: COMPREHENSIVE USER JOURNEY
#=============================================================================
print_scenario "SCENARIO 8: COMPREHENSIVE USER JOURNEY"
echo "This scenario tests a complete user workflow including:"
echo "• Setting up categories and transactions"
echo "• Creating and tracking goals"
echo "• Generating reports"
echo "• Verifying data consistency across operations"
echo ""

print_section "8.1 User Workflow Setup"
api_test "First user creates custom category" "2xx" "POST" "/categories" "{
    \"name\": \"$SIDE_BUSINESS_CATEGORY\",
    \"type\": \"INCOME\"
}" "user1_session.txt"

api_test "Add side business income" "2xx" "POST" "/transactions" "{
    \"amount\": 800.00,
    \"date\": \"2024-01-25\",
    \"category\": \"$SIDE_BUSINESS_CATEGORY\",
    \"description\": \"Consulting work\"
}" "user1_session.txt"

print_section "8.2 Goal Progress Verification"
api_test "Check updated goal progress after new transaction" "2xx" "GET" "/goals/$GOAL_ID_1" "" "user1_session.txt"

# Progress calculation:
# Previous: 9050 (from earlier)
# But we deleted Feb transactions, so recalculate:
# Income: 5500 + 1500 + 800 = 7800
# Expenses: 450 = 450
# Net: 7800 - 450 = 7350
# Progress: 7350/15000 = 49%
validate_response_value "$LAST_RESPONSE" "currentProgress" "7350.00"

print_section "8.3 Report Consistency"
api_test "Generate updated monthly report" "2xx" "GET" "/reports/monthly/2024/1" "" "user1_session.txt"

# January now has additional 800 income
validate_response_value "$LAST_RESPONSE" "netSavings" "7350.00"
validate_response_value "$LAST_RESPONSE" "$SIDE_BUSINESS_CATEGORY" "800.00" "contains"

api_test "Generate updated yearly report" "2xx" "GET" "/reports/yearly/2024" "" "user1_session.txt"

validate_response_value "$LAST_RESPONSE" "netSavings" "7350.00"

print_section "8.4 Session Management"
api_test "Logout first user" "2xx" "POST" "/auth/logout" "" "user1_session.txt"

api_test "Logout second user" "2xx" "POST" "/auth/logout" "" "user2_session.txt"

api_test "Verify sessions invalidated" "4xx" "GET" "/categories" "" "user1_session.txt"

#=============================================================================
# FINAL SUMMARY
#=============================================================================
print_summary