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

  function open() {
    if (typeof modal.showModal === "function") {
      modal.showModal();
    } else {
      toast("Your browser doesn’t support dialogs. Please update to a modern browser.");
    }
  }

  $$("[data-open-modal]").forEach((btn) => btn.addEventListener("click", open));

  const fakeSubmit = $("#fakeSubmit");
  if (fakeSubmit) {
    fakeSubmit.addEventListener("click", (e) => {
      e.preventDefault();
      modal.close();
      toast("You’re on the list. We’ll email you soon.");
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

