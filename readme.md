# glosa poc

## development

### self-signed certificate creation (local vscode live server)

<https://stackoverflow.com/questions/61214717/how-to-enable-https-for-vs-code-live-server-extension>

```sh
openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes \
  -keyout example.key -out example.crt -subj "/CN=example.com" \
  -addext "subjectAltName=DNS:example.com,DNS:www.example.net,IP:10.0.0.1"
```

### publish https server via localtunnel.me

<https://github.com/localtunnel/localtunnel>

npm install -g localtunnel

lt --port 5000 --subdomain my-crazy-subdomain-xxx --local-https true --allow-invalid-cert true --print-requests true
