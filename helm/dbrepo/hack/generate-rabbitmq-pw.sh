#!/bin/bash
# https://stackoverflow.com/a/53175209/2634294
# THIS SCRIPT REQUIRES xxd TO BE INSTALLED:
#     DEBIAN: apt install xxd
#     MACOS: brew install coreutils
function encode_password()
{
    SALT=$(od -A n -t x -N 4 /dev/urandom)
    PASS=$SALT$(echo -n $1 | xxd -ps | tr -d '\n' | tr -d ' ')
    PASS=$(echo -n $PASS | xxd -r -p | sha256sum | head -c 128)
    PASS=$(echo -n $SALT$PASS | xxd -r -p | base64 | tr -d '\n')
    echo $PASS
}
encode_password $1