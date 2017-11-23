package tr.org.uni_talk.ui.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import tr.org.uni_talk.R;
import tr.org.uni_talk.pojo.Contact;


public class ContactListAdapter extends AbstractBaseAdapter<Contact> {

    public String TAG = this.getClass().getName();

    public ContactListAdapter(Activity context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        try {
            Contact contact = (Contact) getItem(position);
            if (convertView == null) {
                convertView = getInflater().inflate(R.layout.contact_item, null);
            }

            Log.e(TAG, "getView: " + contact.getName());

            TextView textViewUser = (TextView) convertView.findViewById(R.id.textViewContactUser);
            textViewUser.setText(contact.getName());

            TextView textViewTime = (TextView) convertView.findViewById(R.id.textViewContactStatus);
            textViewTime.setText(contact.getStatus());

            TextView textViewType = (TextView) convertView.findViewById(R.id.textViewContactType);
            textViewType.setVisibility(View.GONE);

            if (contact.isOwner()) {
                textViewType.setVisibility(View.VISIBLE);
            }
        }catch (IndexOutOfBoundsException ioobe){
            ioobe.printStackTrace();
        }

        return convertView;
    }
}