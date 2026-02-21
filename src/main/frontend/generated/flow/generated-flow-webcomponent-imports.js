import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/tooltip/src/vaadin-tooltip.js';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/button/src/vaadin-button.js';
import 'Frontend/generated/jar-resources/buttonFunctions.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/radio-group/src/vaadin-radio-group.js';
import '@vaadin/list-box/src/vaadin-list-box.js';
import '@vaadin/radio-group/src/vaadin-radio-button.js';
import '@vaadin/tabs/src/vaadin-tab.js';
import '@vaadin/checkbox-group/src/vaadin-checkbox-group.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/time-picker/src/vaadin-time-picker.js';
import 'Frontend/generated/jar-resources/vaadin-time-picker/timepickerConnector.js';
import '@vaadin/side-nav/src/vaadin-side-nav.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/virtual-list/src/vaadin-virtual-list.js';
import 'Frontend/generated/jar-resources/virtualListConnector.js';
import '@vaadin/item/src/vaadin-item.js';
import 'Frontend/generated/jar-resources/menubarConnector.js';
import '@vaadin/menu-bar/src/vaadin-menu-bar.js';
import '@vaadin/dialog/src/vaadin-dialog.js';
import '@vaadin/confirm-dialog/src/vaadin-confirm-dialog.js';
import '@vaadin/integer-field/src/vaadin-integer-field.js';
import '@vaadin/password-field/src/vaadin-password-field.js';
import '@vaadin/email-field/src/vaadin-email-field.js';
import '@vaadin/side-nav/src/vaadin-side-nav-item.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import '@vaadin/number-field/src/vaadin-number-field.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/date-picker/src/vaadin-date-picker.js';
import 'Frontend/generated/jar-resources/datepickerConnector.js';
import '@vaadin/text-area/src/vaadin-text-area.js';
import '@vaadin/date-time-picker/src/vaadin-date-time-picker.js';
import '@vaadin/tabs/src/vaadin-tabs.js';
import '@vaadin/select/src/vaadin-select.js';
import 'Frontend/generated/jar-resources/selectConnector.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'd796f8d3f53f2f202e8fe24691c1786f6dfc752c56066ce9fa161b653d3acb1d') {
    pending.push(import('./chunks/chunk-45ce8abda61435460904cd79a8b4975c1da5f1d88af206e779fe214168f4611d.js'));
  }
  if (key === '8e60b8440c4610f3584ef9c67dbf3b84aaa095c8b41557ddedbea225a0f5a37a') {
    pending.push(import('./chunks/chunk-b5a2aff2e441d32ff8faa3fdaa60a1a404c8d74fa0be72da11723f9aa0b2d45a.js'));
  }
  if (key === '88ae096e0420d12333a5752b238a516685863913d12d079d1ad7adf951678b40') {
    pending.push(import('./chunks/chunk-9a08f6ec1202e8c1ae4a207ed5ad4bdf079d7d3ad7b1ad1bc80610caa5d7e42d.js'));
  }
  if (key === 'd7da8fecbd32c08ec8fd0e0bea47bc6576550d4ccd2f57391d99fd81d4db02d3') {
    pending.push(import('./chunks/chunk-ae3fa0e7b5a8a6f5fcac7a73bb14567fe3296be5b60e59de8b3a7fa3697e18ff.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}