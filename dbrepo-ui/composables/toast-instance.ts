import {type ToastPluginApi, type ToastProps, useToast} from 'vue-toast-notification';

const props: ToastProps = {
  position: 'top-right',
  duration: 6000,
  dismissible: false /* allow copy of error message */
}

export const useToastInstance = () => {
  function error(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (document) {
      toast.error(message)
    }
  }

  function success(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (document) {
      toast.success(message)
    }
  }

  function info(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (document) {
      toast.info(message)
    }
  }

  function warning(message: string): void {
    const toast: ToastPluginApi = useToast(props);
    if (document) {
      toast.warning(message)
    }
  }

  return {error, success, info, warning}
};
