package mobile.android.demo.input.method;

import java.util.ArrayList;
import java.util.List;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class SoftKeyboard extends InputMethodService implements
		KeyboardView.OnKeyboardActionListener
{
	static final boolean DEBUG = false;

	static final boolean PROCESS_HARD_KEYS = true;

	private KeyboardView mInputView;
	private CandidateView mCandidateView;
	private CompletionInfo[] mCompletions;

	private StringBuilder mComposing = new StringBuilder();
	private boolean mPredictionOn;
	private boolean mCompletionOn;
	private int mLastDisplayWidth;
	private boolean mCapsLock;
	private long mLastShiftTime;
	private long mMetaState;

	private LatinKeyboard mSymbolsKeyboard;
	private LatinKeyboard mSymbolsShiftedKeyboard;
	private LatinKeyboard mQwertyKeyboard;

	private LatinKeyboard mCurKeyboard;

	private String mWordSeparators;

	@Override
	public void onCreate()
	{
		super.onCreate();
		mWordSeparators = getResources().getString(R.string.word_separators);


                Log.i("onCreate", "start onCreate");
	}

	@Override
	public void onInitializeInterface()
	{
    Log.i("onInitializeInterface", "init onInitializeInterface main board");
		if (mQwertyKeyboard != null)
		{

			int displayWidth = getMaxWidth();
			if (displayWidth == mLastDisplayWidth)
				return;
			mLastDisplayWidth = displayWidth;
		}
		mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
		mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
		mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);


                
	}


	@Override
	public View onCreateInputView()
	{
    Log.i("onCreateInputView", "set main board and return");
		mInputView = (KeyboardView) getLayoutInflater().inflate(R.layout.input,
				null);
		mInputView.setOnKeyboardActionListener(this);
		mInputView.setKeyboard(mQwertyKeyboard);

                
                
		return mInputView;
	}


	@Override
	public View onCreateCandidatesView()
	{
    Log.i("onCreateCandidatesView", "create CandidatesView and return");
		mCandidateView = new CandidateView(this);
		mCandidateView.setService(this);

                
		return mCandidateView;
	}


	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting)
	{
    
    Log.i("onStartInput", "start input");
		super.onStartInput(attribute, restarting);

		mComposing.setLength(0);
		updateCandidates();

		if (!restarting)
		{

			mMetaState = 0;
		}

		mPredictionOn = false;
		mCompletionOn = false;
		mCompletions = null;

		switch (attribute.inputType & EditorInfo.TYPE_MASK_CLASS)
		{
			case EditorInfo.TYPE_CLASS_NUMBER:
			case EditorInfo.TYPE_CLASS_DATETIME:
				mCurKeyboard = mSymbolsKeyboard;
				break;

			case EditorInfo.TYPE_CLASS_PHONE:
				mCurKeyboard = mSymbolsKeyboard;
				break;

			case EditorInfo.TYPE_CLASS_TEXT:
				mCurKeyboard = mQwertyKeyboard;
				mPredictionOn = true;

				int variation = attribute.inputType
						& EditorInfo.TYPE_MASK_VARIATION;
				if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
						|| variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
				{
					mPredictionOn = false;
				}

				if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
						|| variation == EditorInfo.TYPE_TEXT_VARIATION_URI
						|| variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER)
				{
					mPredictionOn = false;
				}

				if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0)
				{
					mPredictionOn = false;
					mCompletionOn = isFullscreenMode();
				}

				updateShiftKeyState(attribute);
				break;

			default:
				mCurKeyboard = mQwertyKeyboard;
				updateShiftKeyState(attribute);
		}

		mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);

                
	}

	@Override
	public void onFinishInput()
	{
    Log.i("onFinishInput", "finish input");
		super.onFinishInput();

		mComposing.setLength(0);
		updateCandidates();

		setCandidatesViewShown(false);

		mCurKeyboard = mQwertyKeyboard;
		if (mInputView != null)
		{
			mInputView.closing();
		}

               
	}

	@Override
	public void onStartInputView(EditorInfo attribute, boolean restarting)
	{
    Log.i("onStartInputView", "start inputview and show");
		super.onStartInputView(attribute, restarting);
		mInputView.setKeyboard(mCurKeyboard);
		//mInputView.closing();

                
	}

	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd,
			int newSelStart, int newSelEnd, int candidatesStart,
			int candidatesEnd)
	{
    Log.i("onUpdateSelection", "update selection");
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
				candidatesStart, candidatesEnd);

		if (mComposing.length() > 0
				&& (newSelStart != candidatesEnd || newSelEnd != candidatesEnd))
		{
    Log.i("true", "update selection");
			mComposing.setLength(0);
			updateCandidates();
			InputConnection ic = getCurrentInputConnection();
			if (ic != null)
			{Log.i("finishComposingText", "update selection");
				ic.finishComposingText();
			}
		}

                
	}

	@Override
	public void onDisplayCompletions(CompletionInfo[] completions)
	{
    Log.i("onDisplayCompletions", "Display Completions");
		if (mCompletionOn)
		{
			mCompletions = completions;
			if (completions == null)
			{
				setSuggestions(null, false, false);
				return;
			}

			List<String> stringList = new ArrayList<String>();
			for (int i = 0; i < (completions != null ? completions.length : 0); i++)
			{
				CompletionInfo ci = completions[i];
				if (ci != null)
					stringList.add(ci.getText().toString());
			}
			setSuggestions(stringList, true, true);
		}

               
	}

	private boolean translateKeyDown(int keyCode, KeyEvent event)
	{
    Log.i("translateKeyDown", "translate KeyDown");
		mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState, keyCode,
				event);
		int c = event.getUnicodeChar(MetaKeyKeyListener
				.getMetaState(mMetaState));
		mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
		InputConnection ic = getCurrentInputConnection();
		if (c == 0 || ic == null)
		{
			return false;
		}

		boolean dead = false;

		if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0)
		{
			dead = true;
			c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
		}

		if (mComposing.length() > 0)
		{
			char accent = mComposing.charAt(mComposing.length() - 1);
			int composed = KeyEvent.getDeadChar(accent, c);

			if (composed != 0)
			{
				c = composed;
				mComposing.setLength(mComposing.length() - 1);
			}
		}

		onKey(c, null);

                
                
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
    Log.i("onKeyDown", ""+keyCode);
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
				if (event.getRepeatCount() == 0 && mInputView != null)
				{
					if (mInputView.handleBack())
					{
						return true;
					}
				}
				break;

			case KeyEvent.KEYCODE_DEL:
				if (mComposing.length() > 0)
				{
					onKey(Keyboard.KEYCODE_DELETE, null);
					return true;
				}
				break;

			case KeyEvent.KEYCODE_ENTER:
				return false;

			default:
				if (PROCESS_HARD_KEYS)
				{
					if (keyCode == KeyEvent.KEYCODE_SPACE
							&& (event.getMetaState() & KeyEvent.META_ALT_ON) != 0)
					{
						InputConnection ic = getCurrentInputConnection();
						if (ic != null)
						{
							ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
							keyDownUp(KeyEvent.KEYCODE_A);
							keyDownUp(KeyEvent.KEYCODE_N);
							keyDownUp(KeyEvent.KEYCODE_D);
							keyDownUp(KeyEvent.KEYCODE_R);
							keyDownUp(KeyEvent.KEYCODE_O);
							keyDownUp(KeyEvent.KEYCODE_I);
							keyDownUp(KeyEvent.KEYCODE_D);
							return true;
						}
					}
					if (mPredictionOn && translateKeyDown(keyCode, event))
					{
						return true;
					}
				}
		}

                
                
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
    Log.i("onKeyUp", ""+keyCode);
		if (PROCESS_HARD_KEYS)
		{
			if (mPredictionOn)
			{
				mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
						keyCode, event);
			}
		}

                
                
		return super.onKeyUp(keyCode, event);
	}

	private void commitTyped(InputConnection inputConnection)
	{
    Log.i("commitTyped", ""+mComposing);
		if (mComposing.length() > 0)
		{
			inputConnection.commitText(mComposing, mComposing.length());
			mComposing.setLength(0);
			updateCandidates();
		}

                
	}

	private void updateShiftKeyState(EditorInfo attr)
	{
    Log.i("updateShiftKeyState", "update Shift Key State");
		if (attr != null && mInputView != null
				&& mQwertyKeyboard == mInputView.getKeyboard())
		{
			int caps = 0;
			EditorInfo ei = getCurrentInputEditorInfo();
			if (ei != null && ei.inputType != EditorInfo.TYPE_NULL)
			{
				caps = getCurrentInputConnection().getCursorCapsMode(
						attr.inputType);
			}
			mInputView.setShifted(mCapsLock || caps != 0);
		}

               
	}

	private boolean isAlphabeta(int code)
	{
                Log.i("isAlphabeta", "check alphabet");
                
                
		if (Character.isLetter(code))
		{
			return true;
		}
		else
		{
			return false;
		}

               
	}

	private void keyDownUp(int keyEventCode)
	{
    Log.i("keyDownUp", "key Down Up");
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(
				new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
                
	}

	private void sendKey(int keyCode)
	{
    Log.i("sendKey", "send Key");
		switch (keyCode)
		{
			case '\n':
				keyDownUp(KeyEvent.KEYCODE_ENTER);
				break;
			default:
				if (keyCode >= '0' && keyCode <= '9')
				{
					keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
				}
				else
				{
					getCurrentInputConnection().commitText(
							String.valueOf((char) keyCode), 1);
				}
				break;
		}
                
	}


	public void onKey(int primaryCode, int[] keyCodes)
	{
		if (isWordSeparator(primaryCode))
		{
			if (mComposing.length() > 0)
			{
				commitTyped(getCurrentInputConnection());
			}
			sendKey(primaryCode);
			updateShiftKeyState(getCurrentInputEditorInfo());

                        Log.i("onKey", "if (isWordSeparator(primaryCode))");
		}
		else if (primaryCode == Keyboard.KEYCODE_DELETE)
		{
			handleBackspace();
                        Log.i("onKey", "Keyboard.KEYCODE_DELETE");
		}
		else if (primaryCode == Keyboard.KEYCODE_SHIFT)
		{
			handleShift();
                        Log.i("onKey", "Keyboard.KEYCODE_SHIFT");
		}
		else if (primaryCode == Keyboard.KEYCODE_CANCEL)
		{
			handleClose();
                        Log.i("onKey", "Keyboard.KEYCODE_CANCEL");
			return;
		}
		else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS)
		{
		}
		else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
				&& mInputView != null)
		{
			Keyboard current = mInputView.getKeyboard();
			if (current == mSymbolsKeyboard
					|| current == mSymbolsShiftedKeyboard)
			{
				current = mQwertyKeyboard;
			}
			else
			{
				current = mSymbolsKeyboard;
			}
			mInputView.setKeyboard(current);
			if (current == mSymbolsKeyboard)
			{
				current.setShifted(false);
			}
                        Log.i("onKey", "Keyboard.KEYCODE_MODE_CHANGE");
		}
		else
		{
                        Log.i("onKey", "handleCharacter");
			handleCharacter(primaryCode, keyCodes);
                       
		}

                
	}

	public void onText(CharSequence text)
	{
    Log.i("onText", "");
		InputConnection ic = getCurrentInputConnection();
		if (ic == null)
			return;
		ic.beginBatchEdit();
		if (mComposing.length() > 0)
		{
			commitTyped(ic);
		}
		ic.commitText(text, 0);
		ic.endBatchEdit();
		updateShiftKeyState(getCurrentInputEditorInfo());
                
	}

	private void updateCandidates()
	{
    Log.i("updateCandidates", "update Candidates");
		if (!mCompletionOn)
		{
			if (mComposing.length() > 0)
			{
				ArrayList<String> list = new ArrayList<String>();
				list.add(mComposing.toString());
				setSuggestions(list, true, true);
			}
			else
			{
				setSuggestions(null, false, false);
			}
		}
                
	}

	public void setSuggestions(List<String> suggestions, boolean completions,
			boolean typedWordValid)
	{
    Log.i("setSuggestions", "set Suggestions");
		if (suggestions != null && suggestions.size() > 0)
		{
			setCandidatesViewShown(true);
		}
		else if (isExtractViewShown())
		{
			setCandidatesViewShown(true);
		}
		if (mCandidateView != null)
		{
			mCandidateView.setSuggestions(suggestions, completions,
					typedWordValid);
		}

                
	}

	private void handleBackspace()
	{
		final int length = mComposing.length();
		if (length > 1)
		{
			mComposing.delete(length - 1, length);
			getCurrentInputConnection().setComposingText(mComposing, 1);
			updateCandidates();
		}
		else if (length > 0)
		{
			mComposing.setLength(0);
			getCurrentInputConnection().commitText("", 0);
			updateCandidates();
		}
		else
		{
			keyDownUp(KeyEvent.KEYCODE_DEL);
		}
		updateShiftKeyState(getCurrentInputEditorInfo());
	}

	private void handleShift()
	{
		if (mInputView == null)
		{
			return;
		}

		Keyboard currentKeyboard = mInputView.getKeyboard();
		if (mQwertyKeyboard == currentKeyboard)
		{
			checkToggleCapsLock();
			mInputView.setShifted(mCapsLock || !mInputView.isShifted());
		}
		else if (currentKeyboard == mSymbolsKeyboard)
		{
			mSymbolsKeyboard.setShifted(true);
			mInputView.setKeyboard(mSymbolsShiftedKeyboard);
			mSymbolsShiftedKeyboard.setShifted(true);
		}
		else if (currentKeyboard == mSymbolsShiftedKeyboard)
		{
			mSymbolsShiftedKeyboard.setShifted(false);
			mInputView.setKeyboard(mSymbolsKeyboard);
			mSymbolsKeyboard.setShifted(false);
		}
	}

	private void handleCharacter(int primaryCode, int[] keyCodes)
	{
		if (isInputViewShown())
		{
			if (mInputView.isShifted())
			{
				primaryCode = Character.toUpperCase(primaryCode);
			}
		}
		if (isAlphabeta(primaryCode) && mPredictionOn)
		{
			mComposing.append((char) primaryCode);
			getCurrentInputConnection().setComposingText(mComposing, 1);
			updateShiftKeyState(getCurrentInputEditorInfo());
			updateCandidates();
		}
		else
		{
			getCurrentInputConnection().commitText(
					String.valueOf((char) primaryCode), 1);
		}
	}

	private void handleClose()
	{
    Log.i("handleClose", "handle close");
		commitTyped(getCurrentInputConnection());
		requestHideSelf(0);
		mInputView.closing();
	}

	private void checkToggleCapsLock()
	{
		long now = System.currentTimeMillis();
		if (mLastShiftTime + 800 > now)
		{
			mCapsLock = !mCapsLock;
			mLastShiftTime = 0;
		}
		else
		{
			mLastShiftTime = now;
		}
	}

	private String getWordSeparators()
	{
    Log.i("getWordSeparators", "get Word Separators");
		return mWordSeparators;
	}

	public boolean isWordSeparator(int code)
	{
		String separators = getWordSeparators();
		return separators.contains(String.valueOf((char) code));
	}

	public void pickDefaultCandidate()
	{
		pickSuggestionManually(0);
	}

	public void pickSuggestionManually(int index)
	{
		if (mCompletionOn && mCompletions != null && index >= 0
				&& index < mCompletions.length)
		{
			CompletionInfo ci = mCompletions[index];
			getCurrentInputConnection().commitCompletion(ci);
			if (mCandidateView != null)
			{
				mCandidateView.clear();
			}
			updateShiftKeyState(getCurrentInputEditorInfo());
		}
		else if (mComposing.length() > 0)
		{
			commitTyped(getCurrentInputConnection());
		}
	}

	public void swipeRight()
	{
		if (mCompletionOn)
		{
			pickDefaultCandidate();
		}
	}

	public void swipeLeft()
	{
		handleBackspace();
	}

	public void swipeDown()
	{
		handleClose();
	}

	public void swipeUp()
	{
	}

	public void onPress(int primaryCode)
	{
	}

	public void onRelease(int primaryCode)
	{
	}

}
