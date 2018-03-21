
declare var i18n: any;

interface Window {
  React: any;
  ReactComponents: {
    [name: string]: any
  };
  AemGlobal: any;
  initReactComponents(): void;

}



export interface Promise {
    then(success: (result: any) => void, error: (e: any) => void): Promise;
}





