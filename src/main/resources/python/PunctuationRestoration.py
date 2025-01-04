import json
import sys
import traceback
from deepmultilingualpunctuation import PunctuationModel

print("Python script starting...", file=sys.stderr)
sys.stderr.flush()

try:
    from deepmultilingualpunctuation import PunctuationModel
    print("Successfully imported deepmultilingualpunctuation", file=sys.stderr)
    model = PunctuationModel()
    print("Model loaded successfully", file=sys.stderr)
except Exception as e:
    print(f"Error loading model: {str(e)}", file=sys.stderr)
    traceback.print_exc(file=sys.stderr)
    sys.exit(1)

sys.stderr.flush()

def main():
    print("Starting main loop", file=sys.stderr)
    sys.stderr.flush()

    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                print("Empty input received, exiting", file=sys.stderr)
                break

            print(f"Received input: {line.strip()}", file=sys.stderr)
            sys.stderr.flush()

            data = json.loads(line)
            text = data.get("text", "")
            taskId = data.get("taskId", "")

            if text == "test" and taskId == "test-init":
                print(json.dumps({"isSuccess": True, "restoredText": "test", "taskId": "test-init"}))
                sys.stdout.flush()
                continue

            print(f"Processing task {taskId}", file=sys.stderr)
            restored_text = restore_punctuation(text)
            output = {"isSuccess": True, "restoredText": restored_text, "taskId": taskId}

        except Exception as e:
            print(f"Error occurred: {str(e)}", file=sys.stderr)
            traceback.print_exc(file=sys.stderr)
            output = {"isSuccess": False, "error": str(e), "taskId": taskId}

        print(json.dumps(output))
        sys.stdout.flush()
        print(f"Response sent for task {taskId}", file=sys.stderr)
        sys.stderr.flush()

def restore_punctuation(text):
    clean_text = model.preprocess(text)
    labeled_words = model.predict(clean_text)
    size = len(labeled_words)
    result = ""
    for i in range(size):
        word = labeled_words[i]
        result += word[0]
        if word[1] == "0":
            result += " "
        elif not str(word[1]).isdigit():
            if i is not size - 1:
                result += word[1] + " "
            else:
                result += word[1]
    return result



if __name__ == "__main__":
    main()