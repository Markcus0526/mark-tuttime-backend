# Swagger UI Configure

## For Windows:
1. Open Notepad as Administrator.
2. Open C:\Windows\System32\drivers\etc\hosts.
3. Add the line:

```text
109.199.112.48 api-gateway
```
4. Save and close.
5. Flush DNS: ipconfig /flushdns

## For Linux/macOS:
1. Open a terminal.
2. Edit /etc/hosts (e.g., sudo nano /etc/hosts).
3. Add the line:
```text
109.199.112.48 api-gateway
```

4. Save and exit.
5. Flush DNS (macOS: sudo dscacheutil -flushcache; Linux: sudo systemctl restart nscd or just restart browser).