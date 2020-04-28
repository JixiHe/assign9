package com.example.bookshelf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;


public class ViewPagerFragment extends Fragment {

    private PagerAdapter pagerAdapter;
    private ArrayList<BookDetailsFragment> list = new ArrayList<>();
    static ViewPagerFragment newInstance() {
        return new ViewPagerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_viewpager,container,false);
        ViewPager viewPager = view.findViewById (R.id.viewpager);

        pagerAdapter = new PagerAdapter (getChildFragmentManager (), list);
        viewPager.setAdapter(pagerAdapter);

        return view;
    }

    void setBooks(ArrayList<Book> books) {
        list.clear();
        for(int i = 0 ; i < books.size(); i++){
            list.add(BookDetailsFragment.newInstance(books.get(i)));

        }
        pagerAdapter.notifyDataSetChanged();
    }


    private static class PagerAdapter extends FragmentPagerAdapter {
        private ArrayList<BookDetailsFragment> pages;
        PagerAdapter(FragmentManager fm, ArrayList<BookDetailsFragment> pages){
            super(fm,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.pages = pages;

        }

        @Override
        public int getItemPosition( Object object) {
            return PagerAdapter.POSITION_NONE;
        }
        @Override
        public Fragment getItem(int i) {
            return pages.get(i);
        }

        @Override
        public int getCount() {
            return pages.size();
        }
    }
}
