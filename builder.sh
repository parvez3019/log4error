config_gpg(){
  if [ "$GPG_SIGNING_KEY" = "" ]; then
    println "ERROR: No GPG_SIGNING_KEY defined"
    exit 200
  fi

  mkdir -p ~/.gnupg/
  print "${GPG_SIGNING_KEY}" | base64 --decode > ~/.gnupg/private.key
  gpg --import ~/.gnupg/private.key
}