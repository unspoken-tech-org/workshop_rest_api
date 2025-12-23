# Custom act runner image with additional tools
# Used for local GitHub Actions testing with act

FROM catthehacker/ubuntu:act-latest

LABEL maintainer="workshop"
LABEL description="Custom act runner with rsync and other tools"

# Install additional tools needed by the workflow
RUN apt-get update && apt-get install -y --no-install-recommends \
    rsync \
    curl \
    wget \
    jq \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

