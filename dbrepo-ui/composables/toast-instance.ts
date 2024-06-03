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

  return {error, success}
};
