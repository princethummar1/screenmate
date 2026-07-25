# ScreenMate

An accountability platform where groups of friends compete to reduce daily screen time via automated screenshot verification.

## 🚀 Running Locally

To run the Next.js development server locally:

1. Copy `.env.example` to `.env.local` and add your Supabase credentials:
   ```bash
   cp .env.example .env.local
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```
4. Open [http://localhost:3000](http://localhost:3000) in your browser.

## 🐳 Running with Docker

You can easily containerize and run ScreenMate using Docker. A `Dockerfile` and `.dockerignore` have been provided.

### 1. Build the Docker Image
From the root of the `screenmate` directory, run:

```bash
docker build -t screenmate-app .
```

### 2. Run the Docker Container
Once built, run the container and expose it on port 3000. Be sure to pass in your Supabase environment variables!

```bash
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_SUPABASE_URL="YOUR_SUPABASE_URL" \
  -e NEXT_PUBLIC_SUPABASE_ANON_KEY="YOUR_ANON_KEY" \
  screenmate-app
```

Now you can open [http://localhost:3000](http://localhost:3000) to see your app running inside a Docker container!

## 🗄️ Database Setup

The required Supabase database schema and Row Level Security (RLS) policies are located in `supabase/schema.sql`. Execute this SQL script in your Supabase project's SQL Editor to create the necessary tables and permissions.


lYkrEGBuTSGP54pa