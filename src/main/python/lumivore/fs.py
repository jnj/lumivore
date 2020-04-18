import os


def eachdir(root):
    """
    Generator for every subdirectory starting at and including the root.
    """
    if os.path.isdir(root):
        yield root
        entries = os.listdir(root)
        for entry in entries:
            fullpath = os.path.join(root, entry)
            for subdir in eachdir(fullpath):
                yield subdir


def eachfile(root, extensions, case_sensitive=False):
    """
    Generator for every file that has a name ending
    with one of the given extensions. This will recursively
    search all child directories under the root.
    """
    for directory in eachdir(root):
        entries = os.listdir(directory)
        for entry in entries:
            fullentry = os.path.join(directory, entry)
            if os.path.isfile(fullentry) and \
                    any(extension_match(fullentry, ext, case_sensitive) for ext in extensions):
                yield fullentry


def extension_match(filepath, extension, case_sensitive):
    if case_sensitive:
        return filepath.endswith(extension)

    lowercasepath = filepath.lower()
    return lowercasepath.endswith(extension.lower())
