#!/bin/bash

initVault() {
  mkdir ~/.jrevolt
  openssl genrsa | openssl pkcs8 -topk8 -nocrypt -out ~/.jrevolt/vault.key
  subject="/CN=$(whoami)@$(hostname)/OU=MyDepartment/O=MyOrganization/L=MyLocation/C=US"
  [ -f ~/.jrevolt/.subject ] && subject="$(cat ~/.jrevolt/.subject)"
  openssl req -new -x509 -days 1095 -subj "$subject" -key ~/.jrevolt/vault.key -out ~/.jrevolt/vault.crt
}

"$@"
