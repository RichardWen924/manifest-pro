# Admin Frontend

Vite + Vue admin console prototype.

## Pages

- Login
- Dashboard placeholder
- User management CRUD prototype
- User data management for uploaded bill of lading records
- Permission management, enabled only for super admin

## Run

```bash
npm install
npm run dev
```

The dev server proxies `/admin/**` to `http://127.0.0.1:8081`.

The admin backend uses `server.servlet.context-path=/admin`, so frontend requests such as `/admin/users` are proxied directly to the admin service.

## API Fallback

The UI calls `/admin/**` through `src/api/adminApi.js`. If the backend endpoint is not ready yet, the API layer falls back to mock data so the prototype remains usable.
