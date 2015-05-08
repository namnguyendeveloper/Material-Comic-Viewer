package com.comicviewer.cedric.comicviewer;

import java.util.Stack;

import it.neokree.materialnavigationdrawer.elements.MaterialSection;

/**
 * Created by C�dric on 8/05/2015.
 */
public class NavigationManager {

    private static NavigationManager mSingleton;
    private Stack<String> mFileNavigationStack;
    private Stack<MaterialSection> mSectionNavigationStack;
    private Stack<String> mCloudStack;
    public final static String ROOT = "root";

    public static NavigationManager getInstance()
    {
        if (mSingleton==null)
        {
            mSingleton = new NavigationManager();
        }
        return mSingleton;
    }

    public NavigationManager()
    {
        mFileNavigationStack = new Stack<>();
        mSectionNavigationStack = new Stack<>();
        mCloudStack = new Stack<>();

        mFileNavigationStack.push(ROOT);
        mCloudStack.push("/");
    }

    public void resetFileStack()
    {
        mFileNavigationStack.clear();
        mFileNavigationStack.push(ROOT);
    }

    public void resetSectionStack()
    {
        mSectionNavigationStack.clear();
    }

    public void resetCloudStack()
    {
        mCloudStack.clear();
        mCloudStack.push("/");
    }

    public void pushPathToFileStack(String path)
    {
        mFileNavigationStack.push(path);
    }

    public void pushToSectionStack(MaterialSection section)
    {
        mSectionNavigationStack.push(section);
    }

    public void pushPathToCloudStack(String path)
    {
        mCloudStack.push(path);
    }
    public String popFromFileStack()
    {
        if (!mFileNavigationStack.empty())
            return mFileNavigationStack.pop();
        else
            return null;
    }

    public MaterialSection popFromSectionStack()
    {
        if (!mSectionNavigationStack.empty())
            return mSectionNavigationStack.pop();
        else
            return null;
    }

    public String popFromCloudStack()
    {
        if (!mCloudStack.empty())
            return mCloudStack.pop();
        else
            return null;
    }

    public String getPathFromFileStack()
    {
        return mFileNavigationStack.peek();
    }

    public MaterialSection getSectionFromSectionStack()
    {
        return mSectionNavigationStack.peek();
    }

    public String getPathFromCloudStack()
    {
        return mCloudStack.peek();
    }

    public boolean fileStackEmpty()
    {
        return mFileNavigationStack.empty();
    }

    public boolean sectionStackEmpty()
    {
        return mSectionNavigationStack.empty();
    }

    public boolean cloudStackEmpty()
    {
        return mCloudStack.empty();
    }
}
