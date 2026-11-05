import {ref, onMounted, onBeforeUnmount} from 'vue';

interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{
    outcome: 'accepted' | 'dismissed';
    platform: string;
  }>;
  prompt(): Promise<void>;
}

/**
 * Gère l'installation de la PWA sur l'appareil (Chrome, Android, Edge, iOS).
 */
export function usePwaInstall() {
  const deferredPrompt = ref<BeforeInstallPromptEvent | null>(null);
  const isInstallable = ref(false);
  const isInstalled = ref(false);
  const isIos = ref(false);

  function checkInstalled(): boolean {
    const isStandalone = window.matchMedia('(display-mode: standalone)').matches;
    const isIosStandalone = (navigator as unknown as {standalone?: boolean}).standalone === true;
    return isStandalone || isIosStandalone;
  }

  function handleBeforeInstallPrompt(e: Event): void {
    e.preventDefault();
    deferredPrompt.value = e as BeforeInstallPromptEvent;
    isInstallable.value = true;
  }

  function handleAppInstalled(): void {
    isInstalled.value = true;
    isInstallable.value = false;
    deferredPrompt.value = null;
  }

  async function install(): Promise<boolean> {
    if (!deferredPrompt.value) return false;
    await deferredPrompt.value.prompt();
    const {outcome} = await deferredPrompt.value.userChoice;
    if (outcome === 'accepted') {
      isInstalled.value = true;
      isInstallable.value = false;
      deferredPrompt.value = null;
      return true;
    }
    return false;
  }

  onMounted(() => {
    isInstalled.value = checkInstalled();
    const userAgent = window.navigator.userAgent.toLowerCase();
    isIos.value = /iphone|ipad|ipod/.test(userAgent);

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.addEventListener('appinstalled', handleAppInstalled);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    window.removeEventListener('appinstalled', handleAppInstalled);
  });

  return {
    isInstallable,
    isInstalled,
    isIos,
    install,
  };
}
