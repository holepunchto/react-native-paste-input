import type { ForwardedRef } from 'react'
import type {
  HostComponent,
  NativeSyntheticEvent,
  TextInputProps
} from 'react-native'
import type { Int32 } from 'react-native/Libraries/Types/CodegenTypes'

export type PasteTextInputInstance = React.ElementRef<
  HostComponent<PasteInputProps>
> & {
    clear: () => void;
    isFocused: () => boolean;
    getNativeRef?: () => React.ElementRef<HostComponent<PasteInputProps>>;
    setSelection: (start: number, end: number) => void;
};

export interface PastedFile {
    fileName: string;
    fileSize: Int32;
    type: string;
    uri: string;
}   

export interface PasteEvent {
    nativeEvent: {
        data: PastedFile[];
        error?: {
          message: string;
        };
    };
}

export interface PasteTextInputLineWrapEvent {
    nativeEvent: {
        target: number
        lineCount: number
        isWrapped: boolean
    }
}

export interface PasteInputProps extends TextInputProps {
    forwardedRef?: ForwardedRef<PasteTextInputInstance>;
    disableCopyPaste?: boolean;
    onPaste(error: string | null | undefined, files: Array<PastedFile>): void;
    submitBehavior?: SubmitBehavior;
    smartPunctuation?: SmartPunctuation;
    onLineWrap?: (
        event: NativeSyntheticEvent<PasteTextInputLineWrapEvent>
    ) => void;
}

export interface RCTPasteInputProps extends TextInputProps {
    disableCopyPaste?: boolean;
    mostRecentEventCount: number;
    onPaste(event: PasteEvent): void;
    smartPunctuation?: SmartPunctuation;
}

export interface PasteInputRef {
    clear(): void;
    isFocused(): boolean;
    focus(): void;
    blur(): void;
    setNativeProps(nativeProps: object): void;
}

export interface TextInputNativeCommands {
    focus: (viewRef: unknown) => void;
    blur: (viewRef: unknown) => void;
    setTextAndSelection: (
      viewRef: unknown,
      mostRecentEventCount: number,
      value: string | null, // in theory this is nullable
      start: number,
      end: number
    ) => void;
}

export interface Selection {
    start: number;
    end?: number | undefined;
}

export type SubmitBehavior = 'submit' | 'blurAndSubmit' | 'newline';
export type SmartPunctuation = 'default' | 'enable' | 'disable';
