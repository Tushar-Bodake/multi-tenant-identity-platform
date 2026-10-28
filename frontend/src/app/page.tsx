import Link from 'next/link';
import {
  ArrowRight,
  ArrowUpRight,
  Check,
  CheckCircle2,
  ChevronRight,
  Fingerprint,
  KeyRound,
  Layers3,
  LockKeyhole,
  Shield,
  ShieldCheck,
  UsersRound,
} from 'lucide-react';

const highlights = [
  { icon: Layers3, title: 'Tenant isolation', detail: 'Keep every organization’s data in its own boundary.' },
  { icon: Fingerprint, title: 'Passkey-ready', detail: 'Offer phishing-resistant sign-in with WebAuthn.' },
  { icon: ShieldCheck, title: 'Verifiable activity', detail: 'Trace security events with tamper-evident audit chains.' },
];

const safeguards = ['Tenant-scoped access', 'Flexible roles & permissions', 'Passwordless authentication'];

export default function LandingPage() {
  return (
    <div className="min-h-screen overflow-hidden bg-white text-slate-950">
      <header className="relative z-10 border-b border-slate-200/70 bg-white/90 backdrop-blur-xl">
        <div className="mx-auto flex h-[76px] max-w-7xl items-center justify-between px-5 sm:px-8">
          <Link href="/" className="group flex items-center gap-3" aria-label="SecureGate home">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-950 text-white shadow-lg shadow-slate-900/15 transition group-hover:-rotate-3">
              <Shield className="h-5 w-5" strokeWidth={2.2} />
            </span>
            <span className="text-[17px] font-bold tracking-tight">securegate<span className="text-sky-600">.</span></span>
          </Link>

          <nav className="hidden items-center gap-8 md:flex" aria-label="Main navigation">
            <a href="#platform" className="text-sm font-medium text-slate-600 transition hover:text-slate-950">Platform</a>
            <a href="#security" className="text-sm font-medium text-slate-600 transition hover:text-slate-950">Security</a>
          </nav>

          <div className="flex items-center gap-3">
            <Link href="/login" className="hidden rounded-lg px-4 py-2.5 text-sm font-semibold text-slate-700 transition hover:bg-slate-100 sm:inline-flex">
              Sign in
            </Link>
            <Link href="/register" className="inline-flex items-center gap-2 rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-slate-950">
              Get started <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </header>

      <main>
        <section className="relative isolate overflow-hidden bg-[#07111f] text-white">
          <div className="hero-grid pointer-events-none absolute inset-0 opacity-60" />
          <div className="pointer-events-none absolute -left-40 top-12 h-[420px] w-[420px] rounded-full bg-sky-500/20 blur-[120px]" />
          <div className="pointer-events-none absolute -right-32 bottom-0 h-[380px] w-[380px] rounded-full bg-indigo-500/20 blur-[120px]" />

          <div className="relative mx-auto grid max-w-7xl items-center gap-16 px-5 pb-20 pt-16 sm:px-8 sm:pb-24 sm:pt-24 lg:grid-cols-[1.02fr_0.98fr] lg:gap-10 lg:py-28">
            <div className="max-w-2xl">
              <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/[0.06] px-3.5 py-2 text-xs font-medium text-slate-200 shadow-inner shadow-white/[0.04]">
                <span className="relative flex h-2 w-2">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-50" />
                  <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
                </span>
                Identity infrastructure for SaaS teams
                <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
              </div>

              <h1 className="max-w-2xl text-[2.8rem] font-semibold leading-[1.06] tracking-[-0.055em] sm:text-6xl lg:text-[4.35rem]">
                One secure home for every <span className="bg-gradient-to-r from-sky-300 via-cyan-200 to-emerald-200 bg-clip-text text-transparent">identity.</span>
              </h1>
              <p className="mt-6 max-w-xl text-base leading-7 text-slate-300 sm:text-lg sm:leading-8">
                Give every customer a safer way in. SecureGate brings tenant isolation, modern authentication, and clear audit trails together in one identity platform.
              </p>

              <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                <Link href="/register" className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-sky-400 px-5 py-3 text-sm font-bold text-slate-950 shadow-lg shadow-sky-950/30 transition hover:-translate-y-0.5 hover:bg-sky-300 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-300">
                  Create your organization <ArrowRight className="h-4 w-4" />
                </Link>
                <Link href="/login" className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-white/15 bg-white/[0.04] px-5 py-3 text-sm font-semibold text-white transition hover:border-white/30 hover:bg-white/[0.09]">
                  Sign in to SecureGate
                </Link>
              </div>

              <div className="mt-9 flex flex-wrap gap-x-6 gap-y-3">
                {safeguards.map((item) => (
                  <span key={item} className="inline-flex items-center gap-2 text-xs font-medium text-slate-300">
                    <CheckCircle2 className="h-4 w-4 text-emerald-300" /> {item}
                  </span>
                ))}
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-[550px] lg:ml-auto">
              <div className="absolute -inset-5 rounded-[2rem] bg-gradient-to-br from-sky-400/20 via-indigo-400/10 to-emerald-300/10 blur-2xl" />
              <div className="relative rounded-[1.35rem] border border-white/10 bg-[#0d1a2a]/95 p-3 shadow-2xl shadow-black/40 sm:p-4">
                <div className="flex items-center justify-between border-b border-white/[0.08] px-3 pb-4 pt-2">
                  <div className="flex items-center gap-3">
                    <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-sky-400/10 text-sky-300">
                      <Shield className="h-4 w-4" />
                    </div>
                    <div>
                      <p className="text-xs font-semibold text-white">Security overview</p>
                      <p className="mt-0.5 text-[10px] text-slate-400">Workspace preview</p>
                    </div>
                  </div>
                  <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-300/15 bg-emerald-300/[0.08] px-2.5 py-1 text-[10px] font-medium text-emerald-200">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-300" /> Protected
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3 p-2 pt-4 sm:gap-4 sm:p-3">
                  <div className="rounded-xl border border-white/[0.08] bg-white/[0.035] p-4">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-medium text-slate-400">Organization members</span>
                      <UsersRound className="h-4 w-4 text-sky-300" />
                    </div>
                    <div className="mt-4 flex items-end justify-between">
                      <span className="text-2xl font-semibold tracking-tight text-white">24</span>
                      <span className="mb-1 inline-flex items-center gap-1 text-[10px] font-medium text-emerald-300"><ArrowUpRight className="h-3 w-3" /> Team access</span>
                    </div>
                  </div>
                  <div className="rounded-xl border border-white/[0.08] bg-white/[0.035] p-4">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-medium text-slate-400">Sign-in methods</span>
                      <KeyRound className="h-4 w-4 text-violet-300" />
                    </div>
                    <div className="mt-4 flex items-end justify-between">
                      <span className="text-2xl font-semibold tracking-tight text-white">3</span>
                      <span className="mb-1 text-[10px] font-medium text-slate-400">Password · SSO · Passkey</span>
                    </div>
                  </div>

                  <div className="col-span-2 rounded-xl border border-white/[0.08] bg-white/[0.035] p-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-xs font-semibold text-white">Tenant isolation</p>
                        <p className="mt-1 text-[10px] text-slate-400">Organization context applied to access</p>
                      </div>
                      <div className="flex h-9 w-9 items-center justify-center rounded-full border border-emerald-300/20 bg-emerald-300/[0.08] text-emerald-300">
                        <LockKeyhole className="h-4 w-4" />
                      </div>
                    </div>
                    <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-white/[0.08]">
                      <div className="h-full w-[88%] rounded-full bg-gradient-to-r from-sky-400 to-emerald-300" />
                    </div>
                    <div className="mt-2 flex justify-between text-[10px] text-slate-500">
                      <span>Workspace security checks</span><span>Ready</span>
                    </div>
                  </div>

                  <div className="col-span-2 rounded-xl border border-white/[0.08] bg-[#091523] p-4">
                    <div className="mb-3 flex items-center justify-between">
                      <span className="text-xs font-semibold text-white">Recent security activity</span>
                      <span className="text-[10px] text-slate-500">Illustrative preview</span>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center gap-3">
                        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-300/10 text-emerald-300"><Check className="h-3.5 w-3.5" /></span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-[11px] font-medium text-slate-200">Passkey added to account</p>
                          <p className="mt-0.5 text-[10px] text-slate-500">Authentication · just now</p>
                        </div>
                        <span className="h-1.5 w-1.5 rounded-full bg-emerald-300" />
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-300/10 text-sky-300"><UsersRound className="h-3.5 w-3.5" /></span>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-[11px] font-medium text-slate-200">Member role updated</p>
                          <p className="mt-0.5 text-[10px] text-slate-500">Access control · 8 min ago</p>
                        </div>
                        <span className="h-1.5 w-1.5 rounded-full bg-sky-300" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div className="absolute -bottom-5 -left-5 hidden items-center gap-3 rounded-xl border border-white/10 bg-[#112033] px-4 py-3 shadow-xl sm:flex">
                <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-300/10 text-emerald-300"><ShieldCheck className="h-4 w-4" /></div>
                <div><p className="text-xs font-semibold text-white">Security, built in</p><p className="mt-0.5 text-[10px] text-slate-400">A safer foundation for every tenant</p></div>
              </div>
            </div>
          </div>
        </section>

        <section id="platform" className="relative bg-white px-5 py-20 sm:px-8 sm:py-24">
          <div className="mx-auto max-w-7xl">
            <div className="mx-auto max-w-2xl text-center">
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-sky-700">One platform, layered protection</p>
              <h2 className="mt-4 text-3xl font-semibold tracking-[-0.04em] text-slate-950 sm:text-4xl">Identity shouldn’t be the hard part.</h2>
              <p className="mt-4 text-base leading-7 text-slate-600">The essentials for managing who gets in, what they can do, and what happened—designed to work together.</p>
            </div>

            <div id="security" className="mt-12 grid gap-5 md:grid-cols-3">
              {highlights.map(({ icon: Icon, title, detail }, index) => (
                <article key={title} className="group rounded-2xl border border-slate-200 bg-white p-7 shadow-[0_8px_30px_-20px_rgba(15,23,42,0.2)] transition duration-200 hover:-translate-y-1 hover:border-sky-200 hover:shadow-[0_20px_45px_-24px_rgba(15,23,42,0.24)]">
                  <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${index === 0 ? 'bg-sky-50 text-sky-700' : index === 1 ? 'bg-violet-50 text-violet-700' : 'bg-emerald-50 text-emerald-700'}`}>
                    <Icon className="h-5 w-5" strokeWidth={1.8} />
                  </div>
                  <div className="mt-7 flex items-center justify-between">
                    <h3 className="text-base font-semibold tracking-tight text-slate-950">{title}</h3>
                    <ArrowRight className="h-4 w-4 text-slate-300 transition group-hover:translate-x-1 group-hover:text-sky-600" />
                  </div>
                  <p className="mt-2 text-sm leading-6 text-slate-600">{detail}</p>
                </article>
              ))}
            </div>

            <div className="mt-10 flex flex-col items-center justify-between gap-5 rounded-2xl bg-slate-50 px-6 py-6 sm:flex-row sm:px-8">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white text-sky-700 shadow-sm ring-1 ring-slate-200"><LockKeyhole className="h-4 w-4" /></div>
                <div><p className="text-sm font-semibold text-slate-900">Built for security-minded teams</p><p className="mt-0.5 text-xs text-slate-500">Modern controls, without stitching together a stack.</p></div>
              </div>
              <Link href="/register" className="inline-flex items-center gap-2 text-sm font-semibold text-sky-800 transition hover:text-sky-600">
                Explore SecureGate <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-5 px-5 py-7 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <Link href="/" className="flex items-center gap-2 text-sm font-semibold text-slate-800">
            <Shield className="h-4 w-4 text-sky-700" /> SecureGate <span className="font-normal text-slate-400">Identity Platform</span>
          </Link>
          <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-500">
            <span>Multi-tenant access</span><span>Passkey-ready</span><span>Audit visibility</span>
          </div>
          <span className="text-xs text-slate-400">&copy; {new Date().getFullYear()} SecureGate</span>
        </div>
      </footer>
    </div>
  );
}
