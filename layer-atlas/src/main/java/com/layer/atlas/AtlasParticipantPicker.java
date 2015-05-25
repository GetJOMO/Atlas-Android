package com.layer.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * @author Oleg Orlov
 * @since 27 Apr 2015
 */
public class AtlasParticipantPicker extends FrameLayout {

    private static final String TAG = AtlasParticipantPicker.class.getSimpleName();
    private static final boolean debug = true;

    // participants picker
    private View rootView;
    private EditText textFilter;
    private ListView contactsList;
    private ViewGroup selectedContactsContainer;

    private BaseAdapter contactsAdapter;
    
    private TreeSet<String> skipUserIds = new TreeSet<String>();
    private ArrayList<Contact> selectedContacts = new ArrayList<Contact>();
    
    private Contact[] allContacts = null;
    private final ArrayList<Contact> contactsToSelect = new ArrayList<Contact>();
    
    // styles
    private int inputTextColor;
    private Typeface inputTextTypeface;
    private int inputTextStyle;
    private int listTextColor;
    private Typeface listTextTypeface;
    private int listTextStyle;
    private int chipBackgroundColor;
    private int chipTextColor;
    private Typeface chipTextTypeface;
    private int chipTextStyle;
    
    public AtlasParticipantPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseStyle(context, attrs);
    }

    public AtlasParticipantPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseStyle(context, attrs);
    }

    public AtlasParticipantPicker(Context context) {
        super(context);
    }

    public void init(final ContactProvider contactProvider, String[] userIdToSkip) {
        if (contactProvider == null) throw new IllegalArgumentException("ContactProvider cannot be null");
        
        LayoutInflater.from(getContext()).inflate(R.layout.atlas_participants_picker, this);
        
        if (userIdToSkip != null) skipUserIds.addAll(Arrays.asList(userIdToSkip));

        this.allContacts = contactProvider.getAll().toArray(new Contact[contactProvider.getAll().size()]);
        Arrays.sort(allContacts, Contact.FIRST_LAST_EMAIL_ASCENDING);
        
        for (Contact contact : allContacts) {
            if (skipUserIds.contains(contact.userId)) continue;
            contactsToSelect.add(contact);
        }

        // START OF -------------------- Participant Picker ----------------------------------------
        this.rootView = this;
        textFilter = (EditText) rootView.findViewById(R.id.atlas_participants_picker_text);
        contactsList = (ListView) rootView.findViewById(R.id.atlas_participants_picker_list);
        selectedContactsContainer = (ViewGroup) rootView.findViewById(R.id.atlas_participants_picker_names);

        if (rootView.getVisibility() == View.VISIBLE) {
            textFilter.requestFocus();
        }

        // log focuses
        final View scroller = rootView.findViewById(R.id.atlas_participants_picker_scroll);
        scroller.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "scroller.onFocusChange() hasFocus: " + hasFocus);
            }
        });
        selectedContactsContainer.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (debug) Log.w(TAG, "names.onFocusChange()    hasFocus: " + hasFocus);
            }
        });

        // If filter.requestFocus is called from .onClickListener - filter receives focus, but
        // NamesLayout receives it immediately after that. So filter lose it.
        // XXX: scroller also receives focus 
        selectedContactsContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (debug) Log.w(TAG, "names.onTouch() event: " + event);
                if (event.getAction() == MotionEvent.ACTION_DOWN) // ACTION_UP never comes if  
                    textFilter.requestFocus(); //   there is no .onClickListener
                return false;
            }
        });

        textFilter.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                View focused = selectedContactsContainer.hasFocus() ? selectedContactsContainer : selectedContactsContainer.findFocus();
                if (debug) Log.w(TAG, "filter.onFocusChange()   hasFocus: " + hasFocus + ", focused: " + focused);
                if (hasFocus) {
                    contactsList.setVisibility(View.VISIBLE);
                }
                v.post(new Runnable() { // check focus runnable
                    @Override
                    public void run() {
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()   filter.focus: " + textFilter.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run()    names.focus: " + selectedContactsContainer.hasFocus());
                        if (debug) Log.w(TAG, "filter.onFocusChange.run() scroller.focus: " + scroller.hasFocus());

                        // check focus is on any descendants and hide list otherwise  
                        View focused = selectedContactsContainer.hasFocus() ? selectedContactsContainer : selectedContactsContainer.findFocus();
                        if (focused == null) {
                            contactsList.setVisibility(View.GONE);
                            textFilter.setText("");
                        }
                    }
                });
            }
        });

        contactsList.setAdapter(contactsAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.atlas_view_participants_picker_convert, parent, false);
                }

                TextView name = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_name);
                TextView avatarText = (TextView) convertView.findViewById(R.id.atlas_view_participants_picker_convert_ava);
                Contact contact = contactsToSelect.get(position);

                name.setText(contact.getFirstAndLast());
                avatarText.setText(contact.getInitials());
                
                // apply styles
                name.setTextColor(listTextColor);
                name.setTypeface(listTextTypeface, listTextStyle);
                avatarText.setTextColor(listTextColor);
                avatarText.setTypeface(listTextTypeface, listTextStyle);
                
                return convertView;
            }

            public long getItemId(int position) {
                return contactsToSelect.get(position).userId.hashCode();
            }

            public Object getItem(int position) {
                return contactsToSelect.get(position);
            }

            public int getCount() {
                return contactsToSelect.size();
            }
        });

        contactsList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Contact contact = contactsToSelect.get(position);
                selectedContacts.add(contact);
                refreshParticipants(selectedContacts);
                textFilter.setText("");
                textFilter.requestFocus();
                filterContacts("");                 // refresh contactList
            }

        });

        // track text and filter contact list
        textFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (debug) Log.w(TAG, "beforeTextChanged() s: " + s + " start: " + start + " count: " + count + " after: " + after);
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debug) Log.w(TAG, "onTextChanged()     s: " + s + " start: " + start + " before: " + before + " count: " + count);

                final String filter = s.toString().toLowerCase();
                filterContacts(filter);
            }

            public void afterTextChanged(Editable s) {
                if (debug) Log.w(TAG, "afterTextChanged()  s: " + s);
            }
        });

        // select last added participant when press "Backspace/Del"
        textFilter.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (debug) Log.w(TAG, "onKey() keyCode: " + keyCode + ", event: " + event);
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && textFilter.getText().length() == 0 && selectedContacts.size() > 0) {

                    selectedContacts.remove(selectedContacts.size() - 1);
                    refreshParticipants(selectedContacts);
                    filterContacts("");
                    textFilter.requestFocus();
                }
                return false;
            }
        });
        // END OF ---------------------- Participant Picker ---------------------------------------- 

        applyStyle();
    }

    public void refreshParticipants(final ArrayList<Contact> selectedContacts) {

        // remove name_converts first. Better to keep editText in place rather than add/remove that force keyboard to blink
        for (int i = selectedContactsContainer.getChildCount() - 1; i >= 0; i--) {
            View child = selectedContactsContainer.getChildAt(i);
            if (child != textFilter) {
                selectedContactsContainer.removeView(child);
            }
        }
        if (debug) Log.w(TAG, "refreshParticipants() childs left: " + selectedContactsContainer.getChildCount());
        for (Contact contactToAdd : selectedContacts) {
            View contactView = LayoutInflater.from(selectedContactsContainer.getContext()).inflate(R.layout.atlas_view_participants_picker_name_convert, selectedContactsContainer, false);

            TextView avaText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_ava);
            avaText.setText(contactToAdd.getInitials());
            TextView nameText = (TextView) contactView.findViewById(R.id.atlas_view_participants_picker_name_convert_name);
            nameText.setText(contactToAdd.getFirstAndLast());
            contactView.setTag(contactToAdd);

            selectedContactsContainer.addView(contactView, selectedContactsContainer.getChildCount() - 1);
            if (debug) Log.w(TAG, "refreshParticipants() child added: " + contactView + ", for: " + contactToAdd);
            
            // apply styles
            avaText.setTextColor(chipTextColor);
            avaText.setTypeface(chipTextTypeface, chipTextStyle);
            nameText.setTextColor(chipTextColor);
            nameText.setTypeface(chipTextTypeface, chipTextStyle);
            View container = contactView.findViewById(R.id.atlas_view_participants_picker_name_convert);
            GradientDrawable drawable = (GradientDrawable) container.getBackground();
            drawable.setColor(chipBackgroundColor);
            
        }
        if (selectedContacts.size() == 0) {
            LayoutParams params = new LayoutParams(textFilter.getLayoutParams());
            params.width = LayoutParams.MATCH_PARENT;
        }
        selectedContactsContainer.requestLayout();
    }
    
    private void filterContacts(final String filter) {
        contactsToSelect.clear();
        for (Contact contact : allContacts) {
            if (selectedContacts.contains(contact)) continue;
            if (skipUserIds.contains(contact.userId)) continue;
        
            if (contact.firstName != null && contact.firstName.toLowerCase().contains(filter)) {
                contactsToSelect.add(contact);
                continue;
            }
            if (contact.lastName != null && contact.lastName.toLowerCase().contains(filter)) {
                contactsToSelect.add(contact);
                continue;
            }
            if (contact.email != null && contact.email.toLowerCase().contains(filter)) {
                contactsToSelect.add(contact);
                continue;
            }
        }
        Collections.sort(contactsToSelect, new Contact.FilteringComparator(filter));
        contactsAdapter.notifyDataSetChanged();
    }

    public void parseStyle(Context context, AttributeSet attrs) {
        TypedArray ta = context.getResources().obtainAttributes(attrs, R.styleable.AtlasParticipantPicker);
        this.inputTextColor = ta.getColor(R.styleable.AtlasParticipantPicker_inputTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.inputTextStyle = ta.getInt(R.styleable.AtlasParticipantPicker_inputTextStyle, Typeface.NORMAL);
        String inputTextTypefaceName = ta.getString(R.styleable.AtlasParticipantPicker_inputTextTypeface); 
        this.inputTextTypeface  = inputTextTypefaceName != null ? Typeface.create(inputTextTypefaceName, inputTextStyle) : null;
        
        this.listTextColor = ta.getColor(R.styleable.AtlasParticipantPicker_listTextColor, context.getResources().getColor(R.color.atlas_text_black));
        this.listTextStyle = ta.getInt(R.styleable.AtlasParticipantPicker_listTextStyle, Typeface.NORMAL);
        String listTextTypefaceName = ta.getString(R.styleable.AtlasParticipantPicker_listTextTypeface); 
        this.listTextTypeface  = listTextTypefaceName != null ? Typeface.create(listTextTypefaceName, inputTextStyle) : null;
        
        this.chipBackgroundColor = ta.getColor(R.styleable.AtlasParticipantPicker_chipBackgroundColor, context.getResources().getColor(R.color.atlas_background_gray)); 
        this.chipTextColor = ta.getColor(R.styleable.AtlasParticipantPicker_chipTextColor, context.getResources().getColor(R.color.atlas_text_black)); 
        this.chipTextStyle = ta.getInt(R.styleable.AtlasParticipantPicker_chipTextStyle, Typeface.NORMAL);
        String chipTextTypefaceName = ta.getString(R.styleable.AtlasParticipantPicker_chipTextTypeface); 
        this.chipTextTypeface  = chipTextTypefaceName != null ? Typeface.create(chipTextTypefaceName, inputTextStyle) : null;
    }
    
    private void applyStyle() {
        refreshParticipants(selectedContacts);
        contactsAdapter.notifyDataSetChanged();
        textFilter.setTextColor(inputTextColor);
        textFilter.setTypeface(inputTextTypeface, inputTextStyle);
    }


    public String[] getSelectedUserIds() {
        String[] userIds = new String[selectedContacts.size()];
        for (int i = 0; i < selectedContacts.size(); i++) {
            userIds[i] = selectedContacts.get(i).userId;
        }
        return userIds;
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            textFilter.requestFocus();
        }
    }

}
