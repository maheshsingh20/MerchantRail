# Prerequisites and Setup

## Required Software

### 1. Java Development Kit (JDK) 17+

**Check if installed:**
```bash
java -version
```

**Installation:**
- **Windows**: Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Linux**: `sudo apt install openjdk-17-jdk` (Debian/Ubuntu) or `sudo yum install java-17-openjdk` (RHEL/CentOS)
- **macOS**: `brew install openjdk@17`

Set `JAVA_HOME` environment variable:
```bash
# Windows (PowerShell - Admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.x.x", "Machine")

# Linux/macOS (.bashrc or .zshrc)
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

### 2. Apache Maven 3.8+

**Check if installed:**
```bash
mvn --version
```

**Installation:**

**Windows:**
1. Download from [Maven Downloads](https://maven.apache.org/download.cgi)
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH:
```powershell
# PowerShell (Admin)
[System.Environment]::SetEnvironmentVariable("PATH", $env:PATH + ";C:\Program Files\Apache\maven\bin", "Machine")
```

**Linux:**
```bash
sudo apt install maven  # Debian/Ubuntu
sudo yum install maven  # RHEL/CentOS
```

**macOS:**
```bash
brew install maven
```

**Verify installation:**
```bash
mvn --version
# Should show Maven 3.8.x or higher and Java 17+
```

### 3. Docker & Docker Compose

Required for running infrastructure (Postgres, Kafka, Redis) and integration tests.

**Check if installed:**
```bash
docker --version
docker compose version
```

**Installation:**
- **Windows/macOS**: [Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Linux**: Follow [Docker Engine installation](https://docs.docker.com/engine/install/)

**Post-installation (Linux):**
```bash
# Add your user to docker group to run without sudo
sudo usermod -aG docker $USER
newgrp docker
```

### 4. Git

**Check if installed:**
```bash
git --version
```

**Installation:**
- **Windows**: [Git for Windows](https://git-scm.com/download/win)
- **Linux**: `sudo apt install git` or `sudo yum install git`
- **macOS**: `brew install git` or install Xcode Command Line Tools

### 5. Node.js 18+ (For Frontend - Phase 5)

**Check if installed:**
```bash
node --version
npm --version
```

**Installation:**
- Download from [nodejs.org](https://nodejs.org/)
- Or use version manager: [nvm](https://github.com/nvm-sh/nvm)

## Recommended Tools

### IDE
- **IntelliJ IDEA** (Community or Ultimate) - Best for Java/Maven projects
- **VS Code** with Java Extension Pack

### API Testing
- **Postman** or **Insomnia** - For manual API testing
- Alternatively, use `curl` from command line

### Kubernetes (Phase 5)
- **kind** (Kubernetes in Docker): For local K8s cluster testing
  ```bash
  # Windows (PowerShell)
  choco install kind
  
  # Linux
  curl -Lo ./kind https://kind.sigs.k8s.io/dl/latest/kind-linux-amd64
  chmod +x ./kind
  sudo mv ./kind /usr/local/bin/kind
  
  # macOS
  brew install kind
  ```

- **kubectl**: Kubernetes CLI
  ```bash
  # Windows
  choco install kubernetes-cli
  
  # Linux
  curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
  chmod +x kubectl
  sudo mv kubectl /usr/local/bin/
  
  # macOS
  brew install kubectl
  ```

## Verification Script

After installing prerequisites, run this verification script:

```bash
# Save as verify-setup.sh (Linux/macOS) or verify-setup.ps1 (Windows)

echo "Checking prerequisites..."

# Java
java -version 2>&1 | grep -q "version" && echo "✓ Java installed" || echo "✗ Java not found"

# Maven
mvn --version 2>&1 | grep -q "Maven" && echo "✓ Maven installed" || echo "✗ Maven not found"

# Docker
docker --version 2>&1 | grep -q "Docker" && echo "✓ Docker installed" || echo "✗ Docker not found"
docker compose version 2>&1 | grep -q "version" && echo "✓ Docker Compose installed" || echo "✗ Docker Compose not found"

# Git
git --version 2>&1 | grep -q "git version" && echo "✓ Git installed" || echo "✗ Git not found"

echo ""
echo "If any items show ✗, install them using the instructions above."
```

## Quick Start After Setup

Once all prerequisites are installed:

```bash
# Clone the repository
git clone https://github.com/yourusername/merchantrail.git
cd merchantrail

# Build the project
mvn clean install

# Start infrastructure
docker compose up -d

# Run transaction-service (Phase 1)
cd transaction-service
mvn spring-boot:run

# Run all tests
mvn verify
```

## Troubleshooting

### Maven "JAVA_HOME not set" error
Set the `JAVA_HOME` environment variable as described in the JDK section above.

### Docker "permission denied" error (Linux)
Add your user to the docker group:
```bash
sudo usermod -aG docker $USER
newgrp docker
```

### Testcontainers not starting
Ensure Docker is running and accessible:
```bash
docker ps
```

If this fails, start Docker Desktop (Windows/macOS) or start the Docker daemon (Linux).

### Maven download errors
If Maven can't download dependencies, check:
1. Internet connection
2. Firewall/proxy settings
3. Maven settings.xml configuration

---

**Next**: Return to [README.md](../../README.md) to continue with the project.
