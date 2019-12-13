#!/bin/sh

if [ -z "${LFA_BUILDS_SSH_KEY}" ]
then
  echo "LFA_BUILDS_SSH_KEY not set"
  exit 1
fi

if [ -z "${LFA_LAUNCHER_PASSWORD}" ]
then
  echo "LFA_LAUNCHER_PASSWORD not set"
  exit 1
fi

if [ -z "${LFA_KEYSTORE_PASSWORD}" ]
then
  echo "LFA_KEYSTORE_PASSWORD not set"
  exit 1
fi

#------------------------------------------------------------------------
# Configure SSH

mkdir -p "${HOME}/.ssh" || exit 1
echo "${LFA_BUILDS_SSH_KEY}" | base64 -d > "${HOME}/.ssh/id_ed25519" || exit 1
chmod 700 "${HOME}/.ssh" || exit 1
chmod 600 "${HOME}/.ssh/id_ed25519" || exit 1

(cat <<EOF
[builds.lfa.one]:1022 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIH/vroEIxH46lW/xg+CmCDwO7FHN24oP+ad4T/OtB/D2
EOF
) >> "$HOME/.ssh/known_hosts" || exit 1

#------------------------------------------------------------------------
# Configure password

(cat <<EOF
password = ${LFA_LAUNCHER_PASSWORD}
EOF
) >> ./au.org.libraryforall.launcher.app/password.properties || exit 1

#------------------------------------------------------------------------
# Configure Nexus and keystore

scp -B -P 1022 travis-ci@builds.lfa.one:lfa-keystore.jks .

(cat <<EOF

nexusUsername = notausername
nexusPassword = notapassword

au.org.libraryforall.keyAlias=main
au.org.libraryforall.keyPassword=${LFA_KEYSTORE_PASSWORD}
au.org.libraryforall.storePassword=${LFA_KEYSTORE_PASSWORD}
EOF
) >> gradle.properties || exit 1

#------------------------------------------------------------------------
# Build!

./gradlew clean assembleRelease test || exit 1

#------------------------------------------------------------------------
# Publish APKs

scp -B -P 1022 -v ./au.org.libraryforall.launcher.app/build/outputs/apk/release/*.apk travis-ci@builds.lfa.one:/repository/testing/all/ || exit 1