import {type ToastPluginApi, type ToastProps, useToast} from 'vue-toast-notification';

const props: ToastProps = {
  position: 'top-right',
  duration: 6000,
  dismissible: false /* allow copy of error message */
}

export const useToastInstance = () => {
  // The toast library accepts HTML; API messages and interpolated names are plain text.
  function plainText(message: string): string {
    const text = document.createElement('span')
    text.textContent = message
    return text.innerHTML
  }

  function error(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (typeof document !== 'undefined') {
      toast.error(plainText(message))
    }
  }

  function success(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (typeof document !== 'undefined') {
      toast.success(plainText(message))
    }
  }

  function info(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (typeof document !== 'undefined') {
      toast.info(plainText(message))
    }
  }

  function warning(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (typeof document !== 'undefined') {
      toast.warning(plainText(message))
    }
  }

  return {error, success, info, warning}
};
