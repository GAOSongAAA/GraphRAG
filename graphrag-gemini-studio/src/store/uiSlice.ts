import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UiState {
  isSiderCollapsed: boolean;
}

const initialState: UiState = {
  isSiderCollapsed: false,
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSider(state, action: PayloadAction<boolean>) {
      state.isSiderCollapsed = action.payload;
    },
  },
});

export const { toggleSider } = uiSlice.actions;
export default uiSlice.reducer;
