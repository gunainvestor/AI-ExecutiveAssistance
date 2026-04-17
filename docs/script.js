const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

const toastEl = $("#toast");
let toastTimer = null;

function toast(message) {
  if (!toastEl) return;
  toastEl.textContent = message;
  toastEl.classList.add("show");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toastEl.classList.remove("show"), 2400);
}

function scrollToTarget(selector) {
  const el = $(selector);
  if (!el) return;
  el.scrollIntoView({ behavior: "smooth", block: "start" });
}

function initScrollButtons() {
  $$("[data-scroll]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const selector = btn.getAttribute("data-scroll");
      if (selector) scrollToTarget(selector);
    });
  });
}

function initModal() {
  const modal = $("#accessModal");
  if (!modal) return;

  const form = $(".modalInner", modal);
  const submitBtn = $("#fakeSubmit");

  function open() {
    if (typeof modal.showModal === "function") {
      modal.showModal();
    } else {
      toast("Your browser doesn’t support dialogs. Please update to a modern browser.");
    }
  }

  $$("[data-open-modal]").forEach((btn) => btn.addEventListener("click", open));

  async function submitWaitlist() {
    const endpoint = window.__WAITLIST_ENDPOINT__;
    if (!endpoint || String(endpoint).includes("REPLACE-ME")) {
      toast("Waitlist endpoint not configured yet.");
      return;
    }

    const emailInput = $("input[name='email']", modal);
    const nameInput = $("input[name='name']", modal);
    const goalSelect = $("select[name='goal']", modal);

    const email = emailInput?.value?.trim() ?? "";
    const name = nameInput?.value?.trim() ?? "";
    const goal = goalSelect?.value?.trim() ?? "";

    if (!email || !email.includes("@")) {
      toast("Please enter a valid email.");
      return;
    }

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Joining…";
    }

    try {
      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          name,
          goal,
          source: "landing_page",
          userAgent: navigator.userAgent,
          page: location.href,
        }),
      });

      if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(text || `HTTP ${res.status}`);
      }

      modal.close();
      toast("You’re on the list. We’ll email you soon.");
      form?.reset?.();
    } catch (err) {
      toast("Couldn’t join right now. Please try again.");
      console.error(err);
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = "Join waitlist";
      }
    }
  }

  if (form) {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      submitWaitlist();
    });
  }
}

function initFooterYear() {
  const yearEl = $("#year");
  if (yearEl) yearEl.textContent = String(new Date().getFullYear());
}

initScrollButtons();
initModal();
initFooterYear();

