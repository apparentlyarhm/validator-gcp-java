# THIS SCRIPT HAS TO BE ADAPTED TO BASH FOR GITHUB ACTIONS BASED PIPELINES

function Check-EmptyVars {
    param (
        [string[]]$vars
    )
    foreach ($var in $vars) {
        if ([string]::IsNullOrWhiteSpace((Get-Variable -Name $var -ValueOnly))) {
            Write-Host "ERROR: Environment variable '$var' is missing or empty!" -ForegroundColor Red
            exit 1
        }
    }
}

Write-Host "Local CI/CD (sort of) for testing..." -ForegroundColor Cyan

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running. Please start Docker and try again." -ForegroundColor Red
    exit 1
}

# Load .env file and set environment variables
Get-Content .env | ForEach-Object {
    if ($_ -match "^(.*)=(.*)$") {
        $envName = $matches[1].Trim()
        $envValue = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($envName, $envValue, "Process")
    }
}

$GOOGLE_CLOUD_PROJECT = $env:GOOGLE_CLOUD_PROJECT
$GOOGLE_CLOUD_VM_REGION = $env:GOOGLE_CLOUD_VM_REGION
$GOOGLE_CLOUD_AR_REPO_NAME = $env:GOOGLE_CLOUD_AR_REPO_NAME
$GOOGLE_CLOUD_CR_SERVICE_NAME=$env:GOOGLE_CLOUD_CR_SERVICE_NAME
$GOOGLE_CLOUD_FIREWALL_NAME=$env:GOOGLE_CLOUD_FIREWALL_NAME
$GOOGLE_CLOUD_VM_NAME=$env:GOOGLE_CLOUD_VM_NAME
$MINECRAFT_SERVER_PORT=$env:MINECRAFT_SERVER_PORT
$GOOGLE_CLOUD_VM_ZONE=$env:GOOGLE_CLOUD_VM_ZONE

Check-EmptyVars @(
    "GOOGLE_CLOUD_PROJECT"
    "GOOGLE_CLOUD_VM_REGION"
    "GOOGLE_CLOUD_AR_REPO_NAME"
    "GOOGLE_CLOUD_CR_SERVICE_NAME"
    "GOOGLE_CLOUD_FIREWALL_NAME"
    "GOOGLE_CLOUD_VM_NAME"
    "MINECRAFT_SERVER_PORT"
    "GOOGLE_CLOUD_VM_ZONE"
)

Write-Host "All required environment variables are set!" -ForegroundColor Green

$SERVICE_ACCOUNT = "cloud-run-sa@$GOOGLE_CLOUD_PROJECT.iam.gserviceaccount.com"
Write-Host "Using Service Account: $SERVICE_ACCOUNT"

$IMAGE_NAME = "$GOOGLE_CLOUD_VM_REGION-docker.pkg.dev/$GOOGLE_CLOUD_PROJECT/$GOOGLE_CLOUD_AR_REPO_NAME/$GOOGLE_CLOUD_CR_SERVICE_NAME`:latest"
Write-Host "Using Image Name: $IMAGE_NAME"

Write-Host "Authenticating with Google Cloud..."

Write-Host "skipping gcloud auth login.."
#gcloud auth login
gcloud auth configure-docker $GOOGLE_CLOUD_VM_REGION-docker.pkg.dev

# Currently in my local the authentication is done properly, so no need. keeping it here for reference
#Write-Host "Setting GCP Project..."
#gcloud config set project $PROJECT_ID

docker build `
 --build-arg GOOGLE_CLOUD_FIREWALL_NAME="$GOOGLE_CLOUD_FIREWALL_NAME" `
 --build-arg GOOGLE_CLOUD_PROJECT="$GOOGLE_CLOUD_PROJECT" `
 --build-arg GOOGLE_CLOUD_VM_NAME="$GOOGLE_CLOUD_VM_NAME" `
 --build-arg GOOGLE_CLOUD_VM_ZONE="$GOOGLE_CLOUD_VM_ZONE" `
 --build-arg MINECRAFT_SERVER_PORT="$MINECRAFT_SERVER_PORT" `
 -t "$IMAGE_NAME" .

Write-Host "Pushing image.."
docker push $IMAGE_NAME

# Check if the service account exists
$saExists = gcloud iam service-accounts list --project=$GOOGLE_CLOUD_PROJECT --format="value(email)" | Select-String -Pattern $SERVICE_ACCOUNT

if (-not $saExists) {
  Write-Host "❌ ERROR: Service account $SERVICE_ACCOUNT does not exist. To setup ADC, a service account with permissions is required." -ForegroundColor Red
  Exit 1
}

Write-Host "Deploying to Cloud Run..."
gcloud run deploy $GOOGLE_CLOUD_CR_SERVICE_NAME `
  --image=$IMAGE_NAME `
  --platform=managed `
  --service-account=$SERVICE_ACCOUNT `
  --region=$GOOGLE_CLOUD_VM_REGION `
  --allow-unauthenticated

Write-Host "Deployment completed!"
