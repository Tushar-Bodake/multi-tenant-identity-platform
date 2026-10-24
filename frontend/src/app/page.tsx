import Link from 'next/link';
import { Shield, KeyRound, Lock, Users, FileCheck, Layers, ArrowRight, CheckCircle2 } from 'lucide-react';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
      {/* Navigation */}
      <header className="border-b border-slate-200 bg-white/80 backdrop-blur sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-sky-600 rounded-lg text-white shadow-sm">
              <Shield className="w-5 h-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-slate-900">SecureGate</span>
            <span className="text-xs uppercase tracking-wider font-semibold bg-sky-100 text-sky-800 px-2 py-0.5 rounded-full ml-1">
              Identity Platform
            </span>
          </div>
          <div className="flex items-center gap-4">
            <Link href="/login" className="text-sm font-semibold text-slate-700 hover:text-sky-600">
              Sign In
            </Link>
            <Link href="/register" className="btn-primary text-sm">
              Get Started
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1">
        <section className="py-20 px-4 sm:px-6 lg:px-8 max-w-6xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-sky-50 border border-sky-200 text-sky-700 text-xs font-semibold uppercase tracking-wider mb-6">
            <Shield className="w-3.5 h-3.5" /> Enterprise Multi-Tenant Architecture
          </div>
          <h1 className="text-4xl sm:text-6xl font-extrabold text-slate-900 tracking-tight leading-tight sm:leading-none">
            Next-Gen Identity & Access <br className="hidden sm:inline" />
            <span className="text-sky-600">for Modern Multi-Tenant SaaS</span>
          </h1>
          <p className="mt-6 text-lg sm:text-xl text-slate-600 max-w-3xl mx-auto leading-relaxed">
            SecureGate empowers SaaS applications with multi-tenant isolation, row-level database security,
            WebAuthn passkeys, tamper-evident audit logging, and granular RBAC out of the box.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-4">
            <Link href="/register" className="btn-primary px-6 py-3 text-base flex items-center gap-2">
              Create Organization <ArrowRight className="w-4 h-4" />
            </Link>
            <Link href="/login" className="btn-secondary px-6 py-3 text-base">
              Sign in to Dashboard
            </Link>
          </div>

          <div className="mt-12 grid grid-cols-2 sm:grid-cols-4 gap-4 text-left max-w-4xl mx-auto">
            <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-900 text-sm">PostgreSQL RLS</p>
                <p className="text-xs text-slate-500">Kernel-enforced tenant isolation</p>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-900 text-sm">FIDO2 / WebAuthn</p>
                <p className="text-xs text-slate-500">Hardware-backed passkeys</p>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-900 text-sm">Tamper-Evident</p>
                <p className="text-xs text-slate-500">Cryptographic audit log chains</p>
              </div>
            </div>
            <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-sm flex items-start gap-3">
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-slate-900 text-sm">OAuth & Magic Links</p>
                <p className="text-xs text-slate-500">Flexible auth flows</p>
              </div>
            </div>
          </div>
        </section>

        {/* Feature Cards */}
        <section className="py-16 bg-white border-y border-slate-200">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center mb-12">
              <h2 className="text-2xl font-bold text-slate-900">Security Architecture Built for Zero Trust</h2>
              <p className="text-slate-600 text-sm mt-1">Multi-layer protection across database, application, and network layers</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className="card-container hover:shadow-md transition">
                <div className="p-3 bg-sky-50 text-sky-600 rounded-xl w-fit mb-4">
                  <Layers className="w-6 h-6" />
                </div>
                <h3 className="font-semibold text-slate-900 text-lg mb-2">Multi-Tenant Isolation</h3>
                <p className="text-slate-600 text-sm leading-relaxed">
                  Organizations operate within completely isolated boundaries. PostgreSQL Row-Level Security combined with application-level context prevents data leakage across tenants.
                </p>
              </div>

              <div className="card-container hover:shadow-md transition">
                <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl w-fit mb-4">
                  <KeyRound className="w-6 h-6" />
                </div>
                <h3 className="font-semibold text-slate-900 text-lg mb-2">Passwordless & Passkeys</h3>
                <p className="text-slate-600 text-sm leading-relaxed">
                  FIDO2 WebAuthn passkeys eliminate phishing. Single-use cryptographic challenges and asymmetric signature verification ensure secure device enrollment and login.
                </p>
              </div>

              <div className="card-container hover:shadow-md transition">
                <div className="p-3 bg-purple-50 text-purple-600 rounded-xl w-fit mb-4">
                  <FileCheck className="w-6 h-6" />
                </div>
                <h3 className="font-semibold text-slate-900 text-lg mb-2">Tamper-Evident Audit Chains</h3>
                <p className="text-slate-600 text-sm leading-relaxed">
                  Every security-critical event is cryptographically linked with SHA-256 hash chains. Real-time verifiers detect modification, deletion, or tampering instantly.
                </p>
              </div>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <div className="flex items-center gap-2">
            <Shield className="w-4 h-4 text-sky-600" />
            <span className="font-medium text-slate-700">SecureGate Identity Platform</span>
            <span>&copy; {new Date().getFullYear()}</span>
          </div>
          <div className="flex gap-6">
            <span>Argon2id Hashing</span>
            <span>RS256 JWTs</span>
            <span>PostgreSQL RLS</span>
            <span>WebAuthn FIDO2</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
